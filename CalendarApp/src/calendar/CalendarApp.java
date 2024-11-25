package calendar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

// Date 클래스들을 명시적으로 import
import java.sql.Date;
import java.sql.Time;





public class CalendarApp extends javax.swing.JFrame {
    private JLabel monthLabel;
    private JPanel calendarPanel;
    private Calendar calendar;
    private int currentMonth;
    private int currentYear;
    private JTextArea todoArea;
    private final int currentUserId;

    // 데이터베이스 연결 정보
//    private static final String DB_URL = "jdbc:mysql://localhost:3306/calendar_db";
//    private static final String DB_USER = "root"; // MySQL 사용자 이름
//    private static final String DB_PASSWORD = "lo112233.."; // MySQL 비밀번호
    
    private final String DB_URL = "jdbc:mysql://localhost:3306/calendar_db";
    private final String DB_USER = "LeeGH04";  // MySQL 사용자 이름
    private final String DB_PASSWORD = "0004";  // MySQL 비밀번호

    private Connection conn;
    
public CalendarApp(int user_seq) {
    this.currentUserId = user_seq;  // 매개변수로 받은 userId를 저장
    initializeDatabase();
    setTitle("캘린더 & 할 일");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(800, 500);
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
        JLabel todoLabel = new JLabel("할 일 목록", SwingConstants.CENTER);
        todoArea = new JTextArea();
        todoArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(todoArea);

        // 할 일 입력 패널
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField todoInput = new JTextField();
        JButton addButton = new JButton("추가");

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

        inputPanel.add(todoInput, BorderLayout.NORTH);
        inputPanel.add(timePanel, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.SOUTH);

        rightPanel.add(todoLabel, BorderLayout.NORTH);
        rightPanel.add(scrollPane, BorderLayout.CENTER);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);

        // 이벤트 리스너 추가
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
                // util.Date를 sql.Time으로 변환
                java.util.Date startDate = (java.util.Date) startTimeSpinner.getValue();
                java.util.Date endDate = (java.util.Date) endTimeSpinner.getValue();

                // Time 객체 생성
                java.sql.Time startTime = new java.sql.Time(startDate.getTime());
                java.sql.Time endTime = new java.sql.Time(endDate.getTime());

                addTodo(input, startTime, endTime);
                todoInput.setText("");
                updateTodoDisplay();
            }
        });

        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(calendarPanel, BorderLayout.CENTER);

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        add(mainPanel);

        updateCalendar();
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
        // 현재 선택된 날짜의 할 일 목록을 다시 표시
        selectDate(calendar.get(Calendar.DAY_OF_MONTH));
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
        int firstDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDay; i++) {
            calendarPanel.add(new JLabel(""));
        }

        // 날짜별 할 일 개수 조회
        Map<Integer, Integer> todoCount = getTodoCountForMonth();

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

            calendar.set(Calendar.DAY_OF_MONTH, day);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek == Calendar.SUNDAY) {
                dayLabel.setForeground(Color.RED);
            } else if (dayOfWeek == Calendar.SATURDAY) {
                dayLabel.setForeground(Color.BLUE);
            }

            // 할 일이 있는 날짜 표시
            if (todoCount.containsKey(day) && todoCount.get(day) > 0) {
                dayLabel.setBackground(new Color(255, 255, 200));
            } else {
                dayLabel.setBackground(Color.WHITE);
            }

            calendarPanel.add(dayLabel);
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
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

    private void selectDate(int day) {
       calendar.set(currentYear, currentMonth, day);

        // 현재 날짜 및 시간 가져오기
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.sql.Timestamp nowTimestamp = java.sql.Timestamp.valueOf(now);

        // 지난 항목을 완료로 업데이트하는 SQL 쿼리
        String updateSql = "UPDATE todos SET completed = 1 " +
                           "WHERE user_seq = ? AND (date < ? OR (date = ? AND end_time < ?)) AND completed = 0";

        // 선택된 날짜의 할 일 목록을 가져오는 SQL 쿼리
        String selectSql = "SELECT title, start_time, end_time, completed FROM todos " +
                           "WHERE user_seq = ? AND date = ? ORDER BY start_time";

        todoArea.setText(String.format("=== %d년 %d월 %d일 할 일 ===\n", currentYear, currentMonth + 1, day));

        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            // 업데이트 쿼리 실행
            updateStmt.setInt(1, currentUserId);
            updateStmt.setDate(2, java.sql.Date.valueOf(now.toLocalDate())); // 현재 날짜 이전
            updateStmt.setDate(3, java.sql.Date.valueOf(now.toLocalDate())); // 현재 날짜와 동일
            updateStmt.setTime(4, java.sql.Time.valueOf(now.toLocalTime())); // 현재 시간보다 이전
            updateStmt.executeUpdate();

            // 조회 쿼리 실행
            selectStmt.setInt(1, currentUserId);
            selectStmt.setDate(2, java.sql.Date.valueOf(String.format("%d-%02d-%02d",
                    currentYear, currentMonth + 1, day)));

            ResultSet rs = selectStmt.executeQuery();
            int count = 1;
            while (rs.next()) {
                String title = rs.getString("title");
                Time startTime = rs.getTime("start_time");
                Time endTime = rs.getTime("end_time");
                boolean completed = rs.getBoolean("completed"); // TINYINT를 Boolean으로 처리

                String status = completed ? "[완료]" : "[진행중]";
                todoArea.append(String.format("%d. %s %s (%s-%s)\n",
                        count++, status, title,
                        startTime.toString().substring(0, 5),
                        endTime.toString().substring(0, 5)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            todoArea.append("할 일 목록을 불러오는데 실패했습니다.\n");
        }
    }

    private void addTodo(String title, java.sql.Time startTime, java.sql.Time endTime) {
        String sql = "INSERT INTO todos (user_seq, title, date, start_time, end_time) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            pstmt.setString(2, title);
            pstmt.setDate(3, new java.sql.Date(calendar.getTime().getTime()));
            pstmt.setTime(4, startTime);
            pstmt.setTime(5, endTime);

            pstmt.executeUpdate();
            updateCalendar();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "할 일 추가 실패: " + e.getMessage());
        }
    }
//    public static void main(String[] args) {
//    SwingUtilities.invokeLater(() -> {
//        CalendarApp app = new CalendarApp(1); // 테스트용 ID
//        app.setVisible(true);
//    });
//}
}