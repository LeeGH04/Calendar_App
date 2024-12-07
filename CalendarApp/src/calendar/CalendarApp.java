package calendar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;
import javax.swing.Timer;
import java.awt.geom.Arc2D;
import java.time.LocalTime;

// TodoPanel 클래스 정의
class TodoPanel extends JPanel {
    private int todoId;
    private JLabel titleLabel;
    private JLabel timeLabel;
    private JButton deleteButton;
    private JButton editButton;  // 수정 버튼 추가
    
    public TodoPanel(int todoId, String title, Time startTime, Time endTime, 
                    ActionListener deleteListener, ActionListener editListener) {
        this.todoId = todoId;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLineBorder(new Color(200, 200, 200))
        ));
        setBackground(Color.WHITE);

        setPreferredSize(new Dimension(400, 80));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JPanel contentPanel = new JPanel(new GridLayout(2, 1));
        contentPanel.setBackground(Color.WHITE);
        
        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        
        timeLabel = new JLabel(String.format("%s - %s", 
            startTime.toString().substring(0, 5),
            endTime.toString().substring(0, 5)));
        timeLabel.setForeground(Color.GRAY);

        contentPanel.add(titleLabel);
        contentPanel.add(timeLabel);

        // 버튼 패널 추가
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(Color.WHITE);

        // 수정 버튼
        editButton = new JButton("수정");
        editButton.setActionCommand(String.valueOf(todoId));
        editButton.addActionListener(editListener);

        // 삭제 버튼
        deleteButton = new JButton("삭제");
        deleteButton.setActionCommand(String.valueOf(todoId));
        deleteButton.addActionListener(deleteListener);

        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
    }

    public int getTodoId() {
        return todoId;
    }
}

    public class CalendarApp extends javax.swing.JFrame {
    private JLabel monthLabel;
    private JPanel calendarPanel;
    private Calendar calendar;
    private int currentMonth;
    private int currentYear;
    private JPanel todoListPanel;
    private final int currentUserId;
//    private TimeWheel timeWheel;
    private JLabel selectedDateLabel;

    private final String DB_URL = "jdbc:mysql://localhost:3306/calendar_db";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "8421choi@";

    private Connection conn;


    public CalendarApp(int user_seq) {
        this.currentUserId = user_seq;
        initializeDatabase();
        setTitle("캘린더 & 할 일");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);

        // 메인 패널 (캘린더 + 할 일 목록)
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // 왼쪽 패널 (캘린더)
        JPanel leftPanel = new JPanel(new BorderLayout());

        // 상단 패널 생성
        JPanel topPanel = new JPanel();
        JButton prevButton = new JButton("◀");
        JButton nextButton = new JButton("▶");
        monthLabel = new JLabel();

        topPanel.add(prevButton);
        topPanel.add(monthLabel);
        topPanel.add(nextButton);

        // 캘린더 패널 생성
        calendarPanel = new JPanel(new GridLayout(7, 7, 5, 5));

        // 오른쪽 패널 (할 일 목록)
        JPanel rightPanel = new JPanel(new BorderLayout());

        // 오른쪽 상단 패널 (날짜 표시 + 전체 삭제 버튼)
        JPanel headerPanel = new JPanel(new BorderLayout());
        JPanel titlePanel = new JPanel(new BorderLayout());

        // 제목과 날짜 표시
        JLabel todoLabel = new JLabel("할 일 목록",  SwingConstants.CENTER);
        todoLabel.setFont(new Font("맑은 고딕",Font.BOLD,16));
        selectedDateLabel = new JLabel("", SwingConstants.CENTER);
        selectedDateLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        selectedDateLabel.setForeground(Color.BLACK);
        selectedDateLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // 전체 삭제 버튼
        JButton deleteAllButton = new JButton("전체 삭제");
        deleteAllButton.setPreferredSize(new Dimension(80,10));
        deleteAllButton.addActionListener(e -> deleteAllTodos());

        // 패널에 컴포넌트 추가
        titlePanel.add(todoLabel, BorderLayout.NORTH);
        titlePanel.add(selectedDateLabel, BorderLayout.CENTER);

        headerPanel.add(titlePanel, BorderLayout.CENTER);
        headerPanel.add(deleteAllButton, BorderLayout.EAST);

        // TodoPanel을 담을 패널 초기화
        todoListPanel = new JPanel();
        todoListPanel.setLayout(new BoxLayout(todoListPanel, BoxLayout.Y_AXIS));
        todoListPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(todoListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // TimeWheel 초기화
//        timeWheel = new TimeWheel();
//        timeWheel.setPreferredSize(new Dimension(200, 200));

        // 센터 패널
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
//        centerPanel.add(timeWheel, BorderLayout.EAST);

        // 할 일 입력 패널
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField todoInput = new JTextField();

        // 시간 선택 패널
        JPanel timePanel = new JPanel(new FlowLayout());
        JSpinner startTimeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner endTimeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(startTimeSpinner, "HH:mm");
        JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(endTimeSpinner, "HH:mm");
        startTimeSpinner.setEditor(startTimeEditor);
        endTimeSpinner.setEditor(endTimeEditor);

        timePanel.add(new JLabel("시작:"));
        timePanel.add(startTimeSpinner);
        timePanel.add(new JLabel("종료:"));
        timePanel.add(endTimeSpinner);

        // 추가 버튼
        JButton addButton = new JButton("추가");

        inputPanel.add(todoInput, BorderLayout.NORTH);
        inputPanel.add(timePanel, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.SOUTH);

        // right 패널에 컴포넌트 추가
        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(centerPanel, BorderLayout.CENTER);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        // 이벤트 리스너 추가 (기존과 동일)
        prevButton.addActionListener(e -> {
            currentMonth--;
            if (currentMonth < 0) {
                currentMonth = 11;
                currentYear--;
            }
            updateCalendar();
        });

        nextButton.addActionListener(e -> {
            currentMonth++;
            if (currentMonth > 11) {
                currentMonth = 0;
                currentYear++;
            }
            updateCalendar();
        });

        addButton.addActionListener(e -> {
        String input = todoInput.getText().trim();
        if (!input.isEmpty()) {
            java.util.Date startDate = (java.util.Date) startTimeSpinner.getValue();
            java.util.Date endDate = (java.util.Date) endTimeSpinner.getValue();

            java.sql.Time startTime = new java.sql.Time(startDate.getTime());
            java.sql.Time endTime = new java.sql.Time(endDate.getTime());
            
            addTodo(input, startTime, endTime);
            todoInput.setText("");
    }
});

        // 패널 추가
        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(calendarPanel, BorderLayout.CENTER);

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        add(mainPanel);

        updateCalendar();
        updateTodoDisplay();
    }

    private void editTodo(int todoId) {
    // 기존 할 일 정보 가져오기
    String sql = "SELECT title, date, start_time, end_time FROM todos WHERE todo_id = ? AND user_seq = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, todoId);
        pstmt.setInt(2, currentUserId);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            String currentTitle = rs.getString("title");
            Date currentDate = rs.getDate("date");
            Time startTime = rs.getTime("start_time");
            Time endTime = rs.getTime("end_time");

            // 수정 다이얼로그 생성
            JDialog editDialog = new JDialog(this, "할 일 수정", true);
            editDialog.setLayout(new BorderLayout(10, 10));
            editDialog.setSize(400, 200);
            
            JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
            
            // 제목 입력
            JTextField titleField = new JTextField(currentTitle);
            
            // 날짜 선택
            JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
            dateSpinner.setValue(currentDate);
            JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
            dateSpinner.setEditor(dateEditor);
            
            // 시간 선택
            JSpinner startTimeSpinner = new JSpinner(new SpinnerDateModel());
            startTimeSpinner.setValue(startTime);
            JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(startTimeSpinner, "HH:mm");
            startTimeSpinner.setEditor(startTimeEditor);
            
            JSpinner endTimeSpinner = new JSpinner(new SpinnerDateModel());
            endTimeSpinner.setValue(endTime);
            JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(endTimeSpinner, "HH:mm");
            endTimeSpinner.setEditor(endTimeEditor);
            
            inputPanel.add(new JLabel("제목:"));
            inputPanel.add(titleField);
            inputPanel.add(new JLabel("날짜:"));
            inputPanel.add(dateSpinner);
            inputPanel.add(new JLabel("시작 시간:"));
            inputPanel.add(startTimeSpinner);
            inputPanel.add(new JLabel("종료 시간:"));
            inputPanel.add(endTimeSpinner);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveButton = new JButton("저장");
            JButton cancelButton = new JButton("취소");
            
            saveButton.addActionListener(e -> {
                String updateSql = "UPDATE todos SET title = ?, date = ?, start_time = ?, end_time = ? WHERE todo_id = ? AND user_seq = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, titleField.getText());
                    updateStmt.setDate(2, new java.sql.Date(((java.util.Date) dateSpinner.getValue()).getTime()));
                    updateStmt.setTime(3, new java.sql.Time(((java.util.Date) startTimeSpinner.getValue()).getTime()));
                    updateStmt.setTime(4, new java.sql.Time(((java.util.Date) endTimeSpinner.getValue()).getTime()));
                    updateStmt.setInt(5, todoId);
                    updateStmt.setInt(6, currentUserId);
                    
                    int affected = updateStmt.executeUpdate();
                    if (affected > 0) {
                        updateTodoDisplay();
                        updateCalendar();
                        editDialog.dispose();
                        JOptionPane.showMessageDialog(this, "수정되었습니다.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "수정 실패: " + ex.getMessage());
                }
            });
            
            cancelButton.addActionListener(e -> editDialog.dispose());
            
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            
            editDialog.add(inputPanel, BorderLayout.CENTER);
            editDialog.add(buttonPanel, BorderLayout.SOUTH);
            editDialog.setLocationRelativeTo(this);
            editDialog.setVisible(true);
        }
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "데이터 조회 실패: " + e.getMessage());
    }
}
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터베이스 연결 실패: " + e.getMessage());
        }
    }

    private void updateTodoDisplay() {
        todoListPanel.removeAll();
        calendar.set(currentYear, currentMonth, calendar.get(Calendar.DAY_OF_MONTH));
        
        String sql = "SELECT todo_id, title, start_time, end_time FROM todos " +
                     "WHERE user_seq = ? AND date = ? ORDER BY start_time";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            pstmt.setDate(2, new java.sql.Date(calendar.getTime().getTime()));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int todoId = rs.getInt("todo_id");
                String title = rs.getString("title");
                Time startTime = rs.getTime("start_time");
                Time endTime = rs.getTime("end_time");

               TodoPanel todoPanel = new TodoPanel(
                todoId, 
                title, 
                startTime, 
                endTime,
                e -> deleteTodo(Integer.parseInt(e.getActionCommand())),
                e -> editTodo(Integer.parseInt(e.getActionCommand()))  // 수정 버튼 리스너 추가
                );
                todoListPanel.add(todoPanel);
                todoListPanel.add(Box.createRigidArea(new Dimension(0, 5))); // 패널 간 간격
//                timeWheel.addTimeSlot(startTime, endTime);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            String errorMessage = "데이터베이스 오류: " + e.getMessage() + 
                         "\nSQL State: " + e.getSQLState() + 
                         "\nError Code: " + e.getErrorCode();
            JOptionPane.showMessageDialog(this, errorMessage);
}
        
        todoListPanel.revalidate();
        todoListPanel.repaint();
    }

    private void selectDate(int day) {
        calendar.set(currentYear, currentMonth, day);
        updateTodoDisplay();
        
          // 선택한 날짜와 요일 표시
        String[] weekDays = {"일", "월", "화", "수", "목", "금", "토"};
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // Calendar.DAY_OF_WEEK는 1(일요일)부터 시작하므로 -1
        selectedDateLabel.setText(String.format("%d년 %d월 %d일 %s요일", 
            currentYear, currentMonth + 1, day, weekDays[dayOfWeek]));
         
        // 할일이 있는 날짜 정보 가져오기
        Map<Integer, Integer> todoCount = getTodoCountForMonth();
        
        // 선택된 날짜의 라벨에 효과 적용
        for (int i = 7; i < calendarPanel.getComponentCount(); i++) {
        Component comp = calendarPanel.getComponent(i);
        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            try {
                int labelDay = Integer.parseInt(label.getText());
                if (labelDay == day && label.isEnabled()) {
                    // 선택된 날짜에 클릭 효과 애니메이션
                    Timer timer = new Timer(50, new ActionListener() {
                        float alpha = 0.0f;
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            alpha += 0.2f;
                            if (alpha >= 1.0f) {
                                alpha = 1.0f;
                                ((Timer)e.getSource()).stop();
                            }
                            label.setBackground(new Color(135, 206, 250, 
                                (int)(alpha * 128)));
                            label.setBorder(BorderFactory.createLineBorder(
                                new Color(70, 130, 180), 2));
                            label.setOpaque(true);
                            label.repaint();
                        }
                    });
                    timer.start();
                } else if (label.isEnabled()) {
                    // 선택되지 않은 날짜는 할일 유무에 따라 배경색 설정
                    if (todoCount.containsKey(labelDay) && todoCount.get(labelDay) > 0) {
                        label.setBackground(new Color(255, 255, 200));  // 할일이 있으면 노란색 유지
                    } else {
                        label.setBackground(Color.WHITE);  // 할일이 없으면 흰색
                    }
                    label.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                }
            } catch (NumberFormatException e) {
                continue;
            }
        }
    }
        // 할 일 목록 업데이트
        updateTodoDisplay();
    }

    private void addTodo(String title, java.sql.Time startTime, java.sql.Time endTime) {
        String sql = "INSERT INTO todos (user_seq, title, date, start_time, end_time) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        pstmt.setInt(1, currentUserId);
        pstmt.setString(2, title);
        pstmt.setDate(3, new java.sql.Date(calendar.getTime().getTime()));
        pstmt.setTime(4, startTime);
        pstmt.setTime(5, endTime);

        pstmt.executeUpdate();
        
        updateTodoDisplay();
        updateCalendar();  // 달력 업데이트 추가
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "할 일 추가 실패: " + e.getMessage());
    }
    }
    private void deleteTodo(int todoId) {
    int confirm = JOptionPane.showConfirmDialog(this, 
        "정말 삭제하시겠습니까?", 
        "삭제 확인", 
        JOptionPane.YES_NO_OPTION);
        
    if (confirm != JOptionPane.YES_OPTION) {
        return;
    }

     String sql = "DELETE FROM todos WHERE todo_id = ? AND user_seq = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, todoId);
        pstmt.setInt(2, currentUserId);
        
        int affected = pstmt.executeUpdate();
        if (affected > 0) {
            updateTodoDisplay();
            updateCalendar();  // 달력 업데이트 추가
            JOptionPane.showMessageDialog(this, "삭제되었습니다.");
        } else {
            JOptionPane.showMessageDialog(this, "삭제에 실패했습니다.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "삭제 중 오류가 발생했습니다: " + e.getMessage());
    }
}
    
    private void deleteAllTodos() {
    int confirm = JOptionPane.showConfirmDialog(this, 
        "모든 할 일을 삭제하시겠습니까?", 
        "전체 삭제 확인", 
        JOptionPane.YES_NO_OPTION);
        
    if (confirm != JOptionPane.YES_OPTION) {
        return;
    }

    String sql = "DELETE FROM todos WHERE user_seq = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, currentUserId);
        
        int affected = pstmt.executeUpdate();
        if (affected > 0) {
            updateCalendar();
            updateTodoDisplay();
            JOptionPane.showMessageDialog(this, "모든 할 일이 삭제되었습니다.");
        } else {
            JOptionPane.showMessageDialog(this, "삭제할 할 일이 없습니다.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "삭제 중 오류가 발생했습니다: " + e.getMessage());
    }
}
    private Map<Integer, Integer> getTodoCountForMonth() {
    Map<Integer, Integer> todoCount = new HashMap<>();
    String sql = "SELECT DAY(date) as day, COUNT(*) as count FROM todos " +
                "WHERE user_seq = ? AND YEAR(date) = ? AND MONTH(date) = ? " +
                "GROUP BY DAY(date)";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, currentUserId);
        pstmt.setInt(2, currentYear);
        pstmt.setInt(3, currentMonth + 1);

        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            todoCount.put(rs.getInt("day"), rs.getInt("count"));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return todoCount;
}
private void updateCalendar() {
    calendarPanel.removeAll();
    monthLabel.setText(String.format("%d년 %d월", currentYear, currentMonth + 1));

    String[] weekDays = {"일", "월", "화", "수", "목", "금", "토"};
    for (String day : weekDays) {
        JLabel label = new JLabel(day, SwingConstants.CENTER);
        if (day.equals("일")) {
            label.setForeground(Color.RED);
        } else if (day.equals("토")) {
            label.setForeground(Color.BLUE);
        }
        calendarPanel.add(label);
    }

    calendar.set(currentYear, currentMonth, 1);
    int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    
    calendar.add(Calendar.MONTH, -1);
    int prevMonthDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    calendar.add(Calendar.MONTH, 1);
    
    for (int i = 1; i < firstDayOfWeek; i++) {
        JLabel emptyLabel = new JLabel(String.valueOf(prevMonthDays - firstDayOfWeek + i + 1), 
                                     SwingConstants.CENTER);
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setEnabled(false);
        calendarPanel.add(emptyLabel);
    }

    Map<Integer, Integer> todoCount = getTodoCountForMonth();

    int dayOfWeek = firstDayOfWeek;
    for (int day = 1; day <= daysInMonth; day++) {
        JLabel dayLabel = new JLabel(String.valueOf(day), SwingConstants.CENTER);
        dayLabel.setOpaque(true);
        dayLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        final int currentDay = day;
        dayLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectDate(currentDay);
            }
        });

        if (dayOfWeek == Calendar.SUNDAY) {
            dayLabel.setForeground(Color.RED);
        } else if (dayOfWeek == Calendar.SATURDAY) {
            dayLabel.setForeground(Color.BLUE);
        }

        if (todoCount.containsKey(day) && todoCount.get(day) > 0) {
            dayLabel.setBackground(new Color(255, 255, 200));
        } else {
            dayLabel.setBackground(Color.WHITE);
        }

        calendarPanel.add(dayLabel);
        
        dayOfWeek++;
        if (dayOfWeek > Calendar.SATURDAY) {
            dayOfWeek = Calendar.SUNDAY;
        }
    }

    int remainingCells = 42 - (firstDayOfWeek - 1 + daysInMonth);
    for (int i = 1; i <= remainingCells; i++) {
        JLabel emptyLabel = new JLabel(String.valueOf(i), SwingConstants.CENTER);
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setEnabled(false);
        calendarPanel.add(emptyLabel);
    }

    calendarPanel.revalidate();
    calendarPanel.repaint();
}

    
    }

