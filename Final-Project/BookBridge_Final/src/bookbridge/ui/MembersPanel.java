package bookbridge.ui;

import bookbridge.model.FacultyMember;
import bookbridge.model.GuestMember;
import bookbridge.model.LibrarySnapshot;
import bookbridge.model.Member;
import bookbridge.model.MembershipType;
import bookbridge.model.StudentMember;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

final class MembersPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final BookBridgeFrame frame;
    private final JTextField search = new JTextField(18);
    private final DefaultTableModel rows = new DefaultTableModel(new String[] {"Member ID", "Name", "Type"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private transient List<Member> members = List.of();

    MembersPanel(BookBridgeFrame frame) {
        super(new BorderLayout(8, 8));
        this.frame = frame;
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton register = frame.action("Register Member");
        register.addActionListener(e -> register());
        JButton find = frame.action("Search");
        find.addActionListener(e -> render());
        JButton refresh = frame.action("Refresh");
        refresh.addActionListener(e -> frame.refresh());
        top.add(register); top.add(search); top.add(find); top.add(refresh);
        add(top, BorderLayout.NORTH);
        JTable table = new JTable(rows);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void register() {
        JTextField id = new JTextField(20), name = new JTextField(20);
        JComboBox<MembershipType> type = new JComboBox<>(MembershipType.values());
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Member ID")); form.add(id);
        form.add(new JLabel("Name")); form.add(name);
        form.add(new JLabel("Membership")); form.add(type);
        if (JOptionPane.showConfirmDialog(this, form, "Register Member", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        String memberId = id.getText(), memberName = name.getText();
        MembershipType membership = (MembershipType) type.getSelectedItem();
        frame.change("Member registered", () -> {
            Member member = switch (membership) {
                case STUDENT -> new StudentMember(memberId, memberName);
                case FACULTY -> new FacultyMember(memberId, memberName);
                case GUEST -> new GuestMember(memberId, memberName);
            };
            return frame.service().registerMember(member).snapshot();
        });
    }

    void show(LibrarySnapshot snapshot) { members = snapshot.members(); render(); }
    private void render() {
        rows.setRowCount(0);
        String id = search.getText().strip();
        for (Member member : members) {
            if (id.isEmpty() || member.getMemberId().equals(id)) {
                rows.addRow(new Object[] {member.getMemberId(), member.getMemberName(), member.getMembershipType()});
            }
        }
    }
}
