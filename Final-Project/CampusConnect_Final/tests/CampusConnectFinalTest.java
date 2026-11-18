import campusconnect.dao.Database;
import campusconnect.app.CampusConnectFrame;
import campusconnect.exception.*;
import campusconnect.model.*;
import campusconnect.service.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.table.*;

public class CampusConnectFinalTest {
    private static int passed, failed;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void fails(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable e) { if (type.isInstance(e)) return; throw new AssertionError("Unexpected exception: " + e); }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static PersistentRequestService fresh() throws Exception {
        return new PersistentRequestService(Files.createTempDirectory("campus-final-test").resolve("test.db"));
    }
    private static void run(String name, Test test) {
        try { test.run(); passed++; System.out.println("PASS " + name); }
        catch (Throwable e) { failed++; System.out.println("FAIL " + name + ": " + e); }
    }
    @FunctionalInterface private interface Test { void run() throws Exception; }
    private static ServiceRequest add(PersistentRequestService s, String id, ServiceCategory category, int priority) {
        return s.add(id, "Student " + id, category, priority, "Need help");
    }

    public static void main(String[] args) throws Exception {
        run("T01 all four category estimates", () -> {
            PersistentRequestService s = fresh();
            check(add(s,"S1",ServiceCategory.ENROLLMENT,1).estimateDays()==3,"Enrollment");
            check(add(s,"S2",ServiceCategory.DOCUMENT,1).estimateDays()==2,"Document");
            check(add(s,"S3",ServiceCategory.PAYMENT,1).estimateDays()==1,"Payment");
            check(add(s,"S4",ServiceCategory.ADVISING,1).estimateDays()==4,"Advising");
        });
        run("T02 priority five adjustment has one-day floor", () -> {
            PersistentRequestService s=fresh();
            check(add(s,"S1",ServiceCategory.ENROLLMENT,5).estimateDays()==2,"Priority adjustment");
            check(add(s,"S2",ServiceCategory.PAYMENT,5).estimateDays()==1,"One day floor");
        });
        run("T03 required fields and priority boundaries", () -> {
            PersistentRequestService s=fresh();
            fails(InvalidRequestException.class,()->s.add("","A",ServiceCategory.DOCUMENT,1,"Concern"));
            fails(InvalidRequestException.class,()->s.add("S1","",ServiceCategory.DOCUMENT,1,"Concern"));
            fails(InvalidRequestException.class,()->s.add("S1","A",ServiceCategory.DOCUMENT,0,"Concern"));
            fails(InvalidRequestException.class,()->s.add("S1","A",ServiceCategory.DOCUMENT,6,"Concern"));
            fails(InvalidRequestException.class,()->s.add("S1","A",ServiceCategory.DOCUMENT,1," "));
            check(s.listAll().isEmpty(),"Invalid rows stored");
        });
        run("T04 add and find by ticket", () -> {
            PersistentRequestService s=fresh(); ServiceRequest r=add(s,"S1",ServiceCategory.DOCUMENT,2);
            check(s.find(r.getTicketId()).getStudent().getId().equals("S1"),"Find ticket");
        });
        run("T05 search by student", () -> {
            PersistentRequestService s=fresh(); add(s,"S1",ServiceCategory.DOCUMENT,2);
            s.add("S1","Student S1",ServiceCategory.PAYMENT,3,"Payment");
            check(s.findByStudent("s1").size()==2,"Student search");
        });
        run("T06 duplicate ticket rejected", () -> {
            PersistentRequestService s=fresh(); ServiceRequest r=add(s,"S1",ServiceCategory.DOCUMENT,2);
            fails(DuplicateTicketException.class,()->s.add(r));
            check(s.listAll().size()==1,"Duplicate stored");
        });
        run("T07 conflicting student name rejected", () -> {
            PersistentRequestService s=fresh(); add(s,"S1",ServiceCategory.DOCUMENT,2);
            fails(InvalidRequestException.class,()->s.add("S1","Different",ServiceCategory.PAYMENT,2,"Concern"));
            check(s.listAll().size()==1,"Conflicting row stored");
        });
        run("T08 FIFO processing survives restart", () -> {
            Path db=Files.createTempDirectory("campus-restart").resolve("db.sqlite");
            PersistentRequestService s=new PersistentRequestService(db);
            ServiceRequest first=add(s,"S1",ServiceCategory.DOCUMENT,1);
            ServiceRequest second=add(s,"S2",ServiceCategory.PAYMENT,5);
            s=new PersistentRequestService(db);
            check(s.processNext().orElseThrow().getTicketId().equals(first.getTicketId()),"First in queue");
            check(s.processNext().orElseThrow().getTicketId().equals(second.getTicketId()),"Second in queue");
        });
        run("T09 process empty queue", () -> check(fresh().processNext().isEmpty(),"Expected empty"));
        run("T10 valid status progression", () -> {
            PersistentRequestService s=fresh(); String id=add(s,"S1",ServiceCategory.DOCUMENT,1).getTicketId();
            s.updateStatus(id,RequestStatus.IN_PROGRESS); s.updateStatus(id,RequestStatus.COMPLETED);
            check(s.find(id).getStatus()==RequestStatus.COMPLETED,"Completion");
        });
        run("T11 invalid transition rejected", () -> {
            PersistentRequestService s=fresh(); String id=add(s,"S1",ServiceCategory.DOCUMENT,1).getTicketId();
            fails(InvalidRequestException.class,()->s.updateStatus(id,RequestStatus.COMPLETED));
            check(s.find(id).getStatus()==RequestStatus.PENDING,"State changed");
        });
        run("T12 delete and not-found", () -> {
            PersistentRequestService s=fresh(); String id=add(s,"S1",ServiceCategory.DOCUMENT,1).getTicketId();
            s.remove(id); fails(RequestNotFoundException.class,()->s.find(id));
            check(s.summary().distinctStudents()==0,"Student count");
        });
        run("T13 sort by priority then days", () -> {
            PersistentRequestService s=fresh(); add(s,"S1",ServiceCategory.ADVISING,1);
            add(s,"S2",ServiceCategory.ADVISING,5); add(s,"S3",ServiceCategory.PAYMENT,5);
            List<ServiceRequest> rows=s.sorted();
            check(rows.get(0).getCategory()==ServiceCategory.PAYMENT,"Days tie break");
            check(rows.get(2).getPriority()==1,"Priority order");
        });
        run("T14 category and status filter", () -> {
            PersistentRequestService s=fresh(); String id=add(s,"S1",ServiceCategory.DOCUMENT,1).getTicketId();
            add(s,"S2",ServiceCategory.PAYMENT,1); s.updateStatus(id,RequestStatus.IN_PROGRESS);
            check(s.filter(ServiceCategory.DOCUMENT,RequestStatus.IN_PROGRESS).size()==1,"Filter hit");
            check(s.filter(ServiceCategory.DOCUMENT,RequestStatus.PENDING).isEmpty(),"Filter miss");
        });
        run("T15 category/status summary and average", () -> {
            PersistentRequestService s=fresh(); add(s,"S1",ServiceCategory.DOCUMENT,1);
            add(s,"S2",ServiceCategory.ADVISING,1);
            RequestSummary summary=s.summary();
            check(summary.total()==2 && summary.distinctStudents()==2,"Counts");
            check(summary.byCategory().get(ServiceCategory.DOCUMENT)==1,"Category");
            check(summary.byStatus().get(RequestStatus.PENDING)==2,"Status");
            check(summary.averageEstimatedDays()==3.0,"Average");
        });
        run("T16 quote-like text is stored literally", () -> {
            PersistentRequestService s=fresh(); String value="' OR 1=1; DROP TABLE students; --";
            ServiceRequest r=s.add("S1","O'Neil",ServiceCategory.DOCUMENT,2,value);
            check(s.find(r.getTicketId()).getConcern().equals(value),"Text changed");
            check(s.listAll().size()==1,"Database damaged");
        });
        run("T17 transaction rollback on SQL failure", () -> {
            Path db=Files.createTempDirectory("campus-rollback").resolve("db.sqlite");
            PersistentRequestService s=new PersistentRequestService(db);
            try (Connection c=new Database(db).open(); Statement st=c.createStatement()) {
                st.execute("CREATE TRIGGER reject_insert BEFORE INSERT ON service_request BEGIN SELECT RAISE(FAIL,'test failure'); END");
            }
            fails(PersistenceException.class,()->s.add("S1","Student S1",ServiceCategory.DOCUMENT,1,"Concern"));
            try (Connection c=new Database(db).open(); Statement st=c.createStatement()) {
                try (ResultSet rows=st.executeQuery("SELECT count(*) FROM students")) {
                    check(rows.next() && rows.getInt(1)==0,"Student insert was not rolled back");
                }
                try (ResultSet rows=st.executeQuery("SELECT count(*) FROM service_request")) {
                    check(rows.next() && rows.getInt(1)==0,"Request insert was not rolled back");
                }
            }
        });
        run("T18 persisted statuses and records survive restart", () -> {
            Path db=Files.createTempDirectory("campus-persist").resolve("db.sqlite");
            PersistentRequestService s=new PersistentRequestService(db);
            String id=add(s,"S1",ServiceCategory.DOCUMENT,1).getTicketId();
            s.updateStatus(id,RequestStatus.IN_PROGRESS);
            s=new PersistentRequestService(db);
            check(s.find(id).getStatus()==RequestStatus.IN_PROGRESS,"Status lost");
        });
        run("T19 repeated refresh is stable", () -> {
            PersistentRequestService s=fresh(); add(s,"S1",ServiceCategory.DOCUMENT,1);
            for (int i=0;i<20;i++) check(s.listAll().size()==1,"Refresh changed state");
        });
        run("T20 database unavailable gives an error", () -> {
            Path directory=Files.createTempDirectory("campus-outage");
            fails(PersistenceException.class,()->new PersistentRequestService(directory));
        });
        run("T21 SwingWorker separates work and UI callback", () -> {
            CountDownLatch done = new CountDownLatch(1);
            boolean[] result = new boolean[2];
            SwingUtilities.invokeAndWait(() -> new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    result[0] = !SwingUtilities.isEventDispatchThread();
                    return null;
                }
                @Override protected void done() {
                    result[1] = SwingUtilities.isEventDispatchThread();
                    done.countDown();
                }
            }.execute());
            check(done.await(5, TimeUnit.SECONDS),"Worker timed out");
            check(result[0] && result[1],"Wrong thread used");
        });
        run("T22 ticket ID breaks complete sort ties", () -> {
            RequestService s=new RequestService();
            Student student=new Student("S1","Student S1");
            Instant now=Instant.now();
            s.add(ServiceRequest.create("CC-Z",student,ServiceCategory.DOCUMENT,3,"A",
                    RequestStatus.PENDING,now,now));
            s.add(ServiceRequest.create("CC-A",student,ServiceCategory.DOCUMENT,3,"B",
                    RequestStatus.PENDING,now,now));
            check(s.sorted().get(0).getTicketId().equals("CC-A"),"Ticket ID tie break");
        });
        run("T23 table category/status filter survives sorting and refresh", () -> {
            SwingUtilities.invokeAndWait(() -> {
                DefaultTableModel model = new DefaultTableModel(
                        new String[] {"Ticket", "Student ID", "Name", "Category", "Priority", "Concern", "Days", "Status"}, 0);
                Object[][] rows = {
                        {"CC-A", "S1", "One", ServiceCategory.ENROLLMENT, 5, "A", 2, RequestStatus.PENDING},
                        {"CC-B", "S2", "Two", ServiceCategory.ENROLLMENT, 1, "B", 3, RequestStatus.COMPLETED},
                        {"CC-C", "S3", "Three", ServiceCategory.PAYMENT, 3, "C", 1, RequestStatus.PENDING},
                        {"CC-D", "S4", "Four", ServiceCategory.ADVISING, 2, "D", 4, RequestStatus.IN_PROGRESS}
                };
                for (Object[] row : rows) model.addRow(row);
                JTable table = new JTable(model);
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                table.setRowSorter(sorter);
                sorter.setRowFilter(CampusConnectFrame.requestFilter(ServiceCategory.ENROLLMENT, null, ""));
                check(table.getRowCount() == 2, "Category should show two Enrollment rows");
                sorter.setRowFilter(CampusConnectFrame.requestFilter(ServiceCategory.ENROLLMENT, RequestStatus.PENDING, ""));
                check(table.getRowCount() == 1, "Combined category and status should show one row");
                sorter.setRowFilter(CampusConnectFrame.requestFilter(null, RequestStatus.PENDING, ""));
                check(table.getRowCount() == 2, "Pending status should show two rows");
                sorter.setRowFilter(CampusConnectFrame.requestFilter(ServiceCategory.ENROLLMENT, null, "S2"));
                sorter.setSortKeys(List.of(new RowSorter.SortKey(4, SortOrder.DESCENDING)));
                check(table.getRowCount() == 1 && table.getValueAt(0, 0).equals("CC-B"),
                        "Search and category should coexist with sorting");
                model.setRowCount(0);
                for (Object[] row : rows) model.addRow(row);
                check(table.getRowCount() == 1 && table.getValueAt(0, 0).equals("CC-B"),
                        "Refresh should retain active search and category filter");
            });
        });
        System.out.println("RESULT " + passed + " passed, " + failed + " failed");
        if (failed>0) System.exit(1);
    }
}
