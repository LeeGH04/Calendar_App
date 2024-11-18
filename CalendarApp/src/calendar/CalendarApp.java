package calendar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CalendarApp extends JFrame {
    private JLabel monthLabel;
    private JPanel calendarPanel;
    private Calendar calendar;
    private int currentMonth;
    private int currentYear;
    private Map<String, List<String>> todoList; // 날짜별 할 일 저장
    private JTextArea todoArea;

    public CalendarApp() {
        setTitle("캘린더 & 할 일");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLayout(new BorderLayout());

        todoList = new HashMap<>();
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

        inputPanel.add(todoInput, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);

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
                addTodo(input);
                todoInput.setText("");
                updateTodoDisplay();
            }
        });

        todoInput.addActionListener(e -> addButton.doClick());

        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(calendarPanel, BorderLayout.CENTER);

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        add(mainPanel);

        updateCalendar();
    }

    private void updateCalendar() {
        calendarPanel.removeAll();

        // 월/년 레이블 업데이트
        monthLabel.setText(String.format("%d년 %d월", currentYear, currentMonth + 1));

        // 요일 헤더 추가
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

        // 달력 날짜 설정
        calendar.set(currentYear, currentMonth, 1);
        int firstDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 빈 공간 추가
        for (int i = 0; i < firstDay; i++) {
            calendarPanel.add(new JLabel(""));
        }

        // 날짜 추가
        for (int day = 1; day <= daysInMonth; day++) {
            JLabel dayLabel = new JLabel(String.valueOf(day), SwingConstants.CENTER);
            dayLabel.setOpaque(true);
            dayLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            // 날짜 클릭 이벤트
            final int currentDay = day;
            dayLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectDate(currentDay);
                }
            });

            // 주말 색상 설정
            calendar.set(Calendar.DAY_OF_MONTH, day);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek == Calendar.SUNDAY) {
                dayLabel.setForeground(Color.RED);
            } else if (dayOfWeek == Calendar.SATURDAY) {
                dayLabel.setForeground(Color.BLUE);
            }

            // 할 일이 있는 날짜 표시
            String dateKey = String.format("%d-%d-%d", currentYear, currentMonth + 1, day);
            if (todoList.containsKey(dateKey) && !todoList.get(dateKey).isEmpty()) {
                dayLabel.setBackground(new Color(255, 255, 200));
            } else {
                dayLabel.setBackground(Color.WHITE);
            }

            calendarPanel.add(dayLabel);
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    private void selectDate(int day) {
        String dateKey = String.format("%d-%d-%d", currentYear, currentMonth + 1, day);
        todoArea.setText(String.format("=== %d년 %d월 %d일 할 일 ===\n", currentYear, currentMonth + 1, day));
        if (todoList.containsKey(dateKey)) {
            List<String> todos = todoList.get(dateKey);
            for (int i = 0; i < todos.size(); i++) {
                todoArea.append(String.format("%d. %s\n", i + 1, todos.get(i)));
            }
        }
    }

    private void addTodo(String todo) {
        calendar.set(currentYear, currentMonth, calendar.get(Calendar.DAY_OF_MONTH));
        String dateKey = String.format("%d-%d-%d", currentYear, currentMonth + 1, calendar.get(Calendar.DAY_OF_MONTH));

        if (!todoList.containsKey(dateKey)) {
            todoList.put(dateKey, new ArrayList<>());
        }
        todoList.get(dateKey).add(todo);
        updateCalendar();
        updateTodoDisplay();
    }

    private void updateTodoDisplay() {
        selectDate(calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CalendarApp app = new CalendarApp();
            app.setVisible(true);
        });
    }
}