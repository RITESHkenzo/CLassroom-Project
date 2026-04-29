import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    //util

    static class DBConnection {
        private static DBConnection instance;
        private Connection connection;

        private static final String URL  = "jdbc:mysql://localhost:3306/cafeteria_db?useSSL=false&serverTimezone=UTC";
        private static final String USER = "root";
        private static final String PASS = "your_password"; // <-- update this

        private DBConnection() throws SQLException {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                this.connection = DriverManager.getConnection(URL, USER, PASS);
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found. Add mysql-connector-j JAR.", e);
            }
        }

        public static DBConnection getInstance() throws SQLException {
            if (instance == null || instance.connection.isClosed())
                instance = new DBConnection();
            return instance;
        }

        public Connection getConnection() { return connection; }
    }

    //model classes

    static class User {
        private int userId; private String username, password, role;

        public User() {}
        public User(int id, String u, String p, String r) { userId=id; username=u; password=p; role=r; }

        public int    getUserId()   { return userId; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole()     { return role; }
        public void   setUserId(int id)      { userId   = id; }
        public void   setUsername(String u)  { username = u; }
        public void   setPassword(String p)  { password = p; }
        public void   setRole(String r)      { role     = r; }

        public boolean validatePassword(String input) { return password.equals(input); }
        public boolean isAdmin()    { return "ADMIN".equals(role); }
        public boolean isOperator() { return "OPERATOR".equals(role); }
    }

    static class MenuItem {
        private int itemId; private String name, category; private double price; private boolean available;

        public MenuItem() {}
        public MenuItem(int id, String n, String c, double p, boolean a) { itemId=id; name=n; category=c; price=p; available=a; }

        public int     getItemId()   { return itemId; }
        public String  getName()     { return name; }
        public String  getCategory() { return category; }
        public double  getPrice()    { return price; }
        public boolean isAvailable() { return available; }
        public void setItemId(int id)        { itemId    = id; }
        public void setName(String n)        { name      = n; }
        public void setCategory(String c)    { category  = c; }
        public void setPrice(double p)       { price     = p; }
        public void setAvailable(boolean a)  { available = a; }

        @Override public String toString() { return name + " (" + category + ") - Rs." + String.format("%.2f", price); }
    }

    static class Customer {
        private int customerId; private String name, phone;

        public Customer() {}
        public Customer(String n, String p)        { name=n; phone=p; }
        public Customer(int id, String n, String p) { customerId=id; name=n; phone=p; }

        public int    getCustomerId() { return customerId; }
        public String getName()       { return name; }
        public String getPhone()      { return phone; }
        public void setCustomerId(int id) { customerId = id; }
        public void setName(String n)     { name  = n; }
        public void setPhone(String p)    { phone = p; }
    }

    static class OrderItem {
        private int orderItemId; private MenuItem menuItem; private int quantity; private double unitPrice;

        public OrderItem() {}
        public OrderItem(MenuItem mi, int qty) { menuItem=mi; quantity=qty; unitPrice=mi.getPrice(); }

        public int      getOrderItemId() { return orderItemId; }
        public MenuItem getMenuItem()    { return menuItem; }
        public int      getQuantity()    { return quantity; }
        public double   getUnitPrice()   { return unitPrice; }
        public double   getLineTotal()   { return unitPrice * quantity; }
        public void setOrderItemId(int id)     { orderItemId = id; }
        public void setMenuItem(MenuItem mi)   { menuItem    = mi; }
        public void setQuantity(int q)         { quantity    = q; }
        public void setUnitPrice(double p)     { unitPrice   = p; }
    }

    static class Order {
        private int orderId; private Customer customer;
        private List<OrderItem> items; private LocalDateTime timestamp; private String status;

        public Order() { items=new ArrayList<>(); timestamp=LocalDateTime.now(); status="PLACED"; }
        public Order(Customer c) { this(); customer=c; }

        public void addItem(OrderItem oi)    { items.add(oi); }
        public void removeItem(OrderItem oi) { items.remove(oi); }
        public double getTotal() { return items.stream().mapToDouble(OrderItem::getLineTotal).sum(); }

        public int             getOrderId()   { return orderId; }
        public Customer        getCustomer()  { return customer; }
        public List<OrderItem> getItems()     { return items; }
        public LocalDateTime   getTimestamp() { return timestamp; }
        public String          getStatus()    { return status; }
        public void setOrderId(int id)            { orderId   = id; }
        public void setCustomer(Customer c)       { customer  = c; }
        public void setItems(List<OrderItem> lst) { items     = lst; }
        public void setTimestamp(LocalDateTime t) { timestamp = t; }
        public void setStatus(String s)           { status    = s; }
    }

    static class Bill {
        private int billId; private Order order;
        private double subtotal, taxAmount, totalAmount; private LocalDateTime billTimestamp;

        public Bill() { billTimestamp = LocalDateTime.now(); }
        public Bill(Order o, double sub, double tax, double total) { this(); order=o; subtotal=sub; taxAmount=tax; totalAmount=total; }

        public int           getBillId()        { return billId; }
        public Order         getOrder()         { return order; }
        public double        getSubtotal()      { return subtotal; }
        public double        getTaxAmount()     { return taxAmount; }
        public double        getTotalAmount()   { return totalAmount; }
        public LocalDateTime getBillTimestamp() { return billTimestamp; }
        public void setBillId(int id)               { billId        = id; }
        public void setOrder(Order o)               { order         = o; }
        public void setSubtotal(double s)           { subtotal      = s; }
        public void setTaxAmount(double t)          { taxAmount     = t; }
        public void setTotalAmount(double t)        { totalAmount   = t; }
        public void setBillTimestamp(LocalDateTime t){ billTimestamp = t; }
    }

    // Dao classes
    static class UserDao {
        private Connection conn() throws SQLException { return DBConnection.getInstance().getConnection(); }

        public User findByUsername(String username) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM users WHERE username=?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return new User(rs.getInt("user_id"), rs.getString("username"),
                            rs.getString("password"), rs.getString("role"));
                }
            }
            return null;
        }

        public void saveUser(User u) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO users(username,password,role) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, u.getUsername()); ps.setString(2, u.getPassword()); ps.setString(3, u.getRole());
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) u.setUserId(k.getInt(1)); }
            }
        }
    }

    static class MenuDao {
        private Connection conn() throws SQLException { return DBConnection.getInstance().getConnection(); }

        public List<MenuItem> getAllItems() throws SQLException {
            List<MenuItem> list = new ArrayList<>();
            try (ResultSet rs = conn().createStatement().executeQuery(
                    "SELECT * FROM menu_items ORDER BY category,name")) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }

        public List<MenuItem> getAvailableItems() throws SQLException {
            List<MenuItem> list = new ArrayList<>();
            try (ResultSet rs = conn().createStatement().executeQuery(
                    "SELECT * FROM menu_items WHERE available=TRUE ORDER BY category,name")) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }

        public MenuItem findById(int id) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM menu_items WHERE item_id=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
            }
            return null;
        }

        public boolean isNameUnique(String name, int excludeId) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement(
                    "SELECT COUNT(*) FROM menu_items WHERE LOWER(name)=LOWER(?) AND item_id!=?")) {
                ps.setString(1, name); ps.setInt(2, excludeId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1) == 0; }
            }
            return true;
        }

        public void addItem(MenuItem mi) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO menu_items(name,category,price,available) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, mi.getName()); ps.setString(2, mi.getCategory());
                ps.setDouble(3, mi.getPrice()); ps.setBoolean(4, mi.isAvailable());
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) mi.setItemId(k.getInt(1)); }
            }
        }

        public void updateItem(MenuItem mi) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement(
                    "UPDATE menu_items SET name=?,category=?,price=?,available=? WHERE item_id=?")) {
                ps.setString(1, mi.getName()); ps.setString(2, mi.getCategory());
                ps.setDouble(3, mi.getPrice()); ps.setBoolean(4, mi.isAvailable()); ps.setInt(5, mi.getItemId());
                ps.executeUpdate();
            }
        }

        public void deleteItem(int id) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement("DELETE FROM menu_items WHERE item_id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
        }

        private MenuItem map(ResultSet rs) throws SQLException {
            return new MenuItem(rs.getInt("item_id"), rs.getString("name"),
                    rs.getString("category"), rs.getDouble("price"), rs.getBoolean("available"));
        }
    }

    static class OrderDao {
        private Connection conn() throws SQLException { return DBConnection.getInstance().getConnection(); }

        public void saveCustomer(Customer c) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO customers(name,phone) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getName()); ps.setString(2, c.getPhone());
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) c.setCustomerId(k.getInt(1)); }
            }
        }

        public int saveOrder(Order order) throws SQLException {
            saveCustomer(order.getCustomer());
            int orderId;
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO orders(customer_id,order_timestamp,status) VALUES(?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, order.getCustomer().getCustomerId());
                ps.setTimestamp(2, Timestamp.valueOf(order.getTimestamp()));
                ps.setString(3, order.getStatus());
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); orderId = k.getInt(1); order.setOrderId(orderId); }
            }
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO order_items(order_id,item_id,quantity,unit_price) VALUES(?,?,?,?)")) {
                for (OrderItem oi : order.getItems()) {
                    ps.setInt(1, orderId); ps.setInt(2, oi.getMenuItem().getItemId());
                    ps.setInt(3, oi.getQuantity()); ps.setDouble(4, oi.getUnitPrice());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return orderId;
        }

        public List<Order> getAllOrders() throws SQLException {
            List<Order> orders = new ArrayList<>();
            String sql = "SELECT o.order_id,o.order_timestamp,o.status,c.customer_id,c.name AS cname,c.phone " +
                         "FROM orders o JOIN customers c ON o.customer_id=c.customer_id ORDER BY o.order_timestamp DESC";
            try (ResultSet rs = conn().createStatement().executeQuery(sql)) {
                while (rs.next()) { Order o = mapOrder(rs); o.setItems(getItems(o.getOrderId())); orders.add(o); }
            }
            return orders;
        }

        public Order getOrderById(int id) throws SQLException {
            String sql = "SELECT o.order_id,o.order_timestamp,o.status,c.customer_id,c.name AS cname,c.phone " +
                         "FROM orders o JOIN customers c ON o.customer_id=c.customer_id WHERE o.order_id=?";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { Order o = mapOrder(rs); o.setItems(getItems(id)); return o; }
                }
            }
            return null;
        }

        public List<Order> getOrdersByDate(LocalDate date) throws SQLException {
            List<Order> orders = new ArrayList<>();
            String sql = "SELECT o.order_id,o.order_timestamp,o.status,c.customer_id,c.name AS cname,c.phone " +
                         "FROM orders o JOIN customers c ON o.customer_id=c.customer_id " +
                         "WHERE DATE(o.order_timestamp)=? ORDER BY o.order_timestamp DESC";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) { Order o = mapOrder(rs); o.setItems(getItems(o.getOrderId())); orders.add(o); }
                }
            }
            return orders;
        }

        public void saveBill(Bill bill) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO bills(order_id,subtotal,tax_amount,total_amount,bill_timestamp) VALUES(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, bill.getOrder().getOrderId()); ps.setDouble(2, bill.getSubtotal());
                ps.setDouble(3, bill.getTaxAmount()); ps.setDouble(4, bill.getTotalAmount());
                ps.setTimestamp(5, Timestamp.valueOf(bill.getBillTimestamp()));
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) bill.setBillId(k.getInt(1)); }
            }
        }

        public boolean isBilled(int orderId) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement("SELECT COUNT(*) FROM bills WHERE order_id=?")) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1) > 0; }
            }
            return false;
        }

        public Bill getBillByOrderId(int orderId) throws SQLException {
            try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM bills WHERE order_id=?")) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Bill b = new Bill();
                        b.setBillId(rs.getInt("bill_id")); b.setSubtotal(rs.getDouble("subtotal"));
                        b.setTaxAmount(rs.getDouble("tax_amount")); b.setTotalAmount(rs.getDouble("total_amount"));
                        b.setBillTimestamp(rs.getTimestamp("bill_timestamp").toLocalDateTime());
                        b.setOrder(getOrderById(orderId));
                        return b;
                    }
                }
            }
            return null;
        }

        private Order mapOrder(ResultSet rs) throws SQLException {
            Customer c = new Customer(rs.getInt("customer_id"), rs.getString("cname"), rs.getString("phone"));
            Order o = new Order(c);
            o.setOrderId(rs.getInt("order_id"));
            o.setTimestamp(rs.getTimestamp("order_timestamp").toLocalDateTime());
            o.setStatus(rs.getString("status"));
            return o;
        }

        private List<OrderItem> getItems(int orderId) throws SQLException {
            List<OrderItem> items = new ArrayList<>();
            String sql = "SELECT oi.order_item_id,oi.quantity,oi.unit_price," +
                         "mi.item_id,mi.name,mi.category,mi.price,mi.available " +
                         "FROM order_items oi JOIN menu_items mi ON oi.item_id=mi.item_id WHERE oi.order_id=?";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MenuItem mi = new MenuItem(rs.getInt("item_id"), rs.getString("name"),
                                rs.getString("category"), rs.getDouble("price"), rs.getBoolean("available"));
                        OrderItem oi = new OrderItem(mi, rs.getInt("quantity"));
                        oi.setOrderItemId(rs.getInt("order_item_id"));
                        oi.setUnitPrice(rs.getDouble("unit_price"));
                        items.add(oi);
                    }
                }
            }
            return items;
        }
    }

    //service classes

    static class UserService {
        private final UserDao dao = new UserDao();
        public User login(String username, String password) throws SQLException {
            User u = dao.findByUsername(username);
            return (u != null && u.validatePassword(password)) ? u : null;
        }
    }

    static class MenuService {
        private final MenuDao dao = new MenuDao();
        public List<MenuItem> getAllItems()       throws SQLException { return dao.getAllItems(); }
        public List<MenuItem> getAvailableItems() throws SQLException { return dao.getAvailableItems(); }

        public void addItem(MenuItem mi) throws SQLException    { validate(mi, true);  dao.addItem(mi); }
        public void updateItem(MenuItem mi) throws SQLException { validate(mi, false); dao.updateItem(mi); }
        public void deleteItem(int id) throws SQLException      { dao.deleteItem(id); }

        public void validate(MenuItem mi, boolean isNew) throws SQLException {
            if (mi.getName()     == null || mi.getName().trim().isEmpty())     throw new IllegalArgumentException("Item name cannot be empty.");
            if (mi.getCategory() == null || mi.getCategory().trim().isEmpty()) throw new IllegalArgumentException("Category cannot be empty.");
            if (mi.getPrice() <= 0)                                            throw new IllegalArgumentException("Price must be greater than zero.");
            int excludeId = isNew ? -1 : mi.getItemId();
            if (!dao.isNameUnique(mi.getName(), excludeId))
                throw new IllegalArgumentException("Item '" + mi.getName() + "' already exists.");
        }
    }

    static class OrderService {
        private final OrderDao dao = new OrderDao();
        public int placeOrder(Order o) throws SQLException { validate(o); return dao.saveOrder(o); }
        public List<Order> getOrderHistory()                     throws SQLException { return dao.getAllOrders(); }
        public Order       getOrderById(int id)                  throws SQLException { return dao.getOrderById(id); }
        public List<Order> getDailySummary(LocalDate d)          throws SQLException { return dao.getOrdersByDate(d); }
        public double getTotalRevenue(List<Order> orders) { return orders.stream().mapToDouble(Order::getTotal).sum(); }

        public void validate(Order o) {
            if (o.getCustomer() == null || o.getCustomer().getName().trim().isEmpty())
                throw new IllegalArgumentException("Customer name is required.");
            if (o.getItems() == null || o.getItems().isEmpty())
                throw new IllegalArgumentException("Order must have at least one item.");
            for (OrderItem oi : o.getItems())
                if (oi.getQuantity() <= 0)
                    throw new IllegalArgumentException("Quantity for '" + oi.getMenuItem().getName() + "' must be at least 1.");
        }
    }

    static class BillingService {
        private static final double TAX_RATE = 0.05;
        private final OrderDao dao = new OrderDao();

        public Bill generateBill(Order order) throws SQLException {
            if (dao.isBilled(order.getOrderId()))
                throw new IllegalStateException("Order #" + order.getOrderId() + " is already billed.");
            double sub   = calculateSubtotal(order);
            double tax   = calculateTax(sub);
            double total = calculateTotal(sub, tax);
            Bill bill = new Bill(order, sub, tax, total);
            dao.saveBill(bill);
            return bill;
        }

        public double calculateSubtotal(Order o) { return o.getItems().stream().mapToDouble(OrderItem::getLineTotal).sum(); }
        public double calculateTax(double sub)   { return Math.round(sub * TAX_RATE * 100.0) / 100.0; }
        public double calculateTotal(double s, double t) { return Math.round((s + t) * 100.0) / 100.0; }

        public String formatBillText(Bill b) {
            StringBuilder sb = new StringBuilder();
            sb.append("========================================\n");
            sb.append("           CAFETERIA BILL\n");
            sb.append("========================================\n");
            sb.append(String.format("Bill ID   : #%d%n",   b.getBillId()));
            sb.append(String.format("Order ID  : #%d%n",   b.getOrder().getOrderId()));
            sb.append(String.format("Customer  : %s%n",    b.getOrder().getCustomer().getName()));
            sb.append(String.format("Date/Time : %s%n",    b.getBillTimestamp().toString().substring(0,19)));
            sb.append("----------------------------------------\n");
            sb.append(String.format("%-20s %5s %10s%n", "Item", "Qty", "Amount"));
            sb.append("----------------------------------------\n");
            for (OrderItem oi : b.getOrder().getItems())
                sb.append(String.format("%-20s %5d %10.2f%n",
                        oi.getMenuItem().getName(), oi.getQuantity(), oi.getLineTotal()));
            sb.append("----------------------------------------\n");
            sb.append(String.format("%-26s %10.2f%n", "Subtotal",  b.getSubtotal()));
            sb.append(String.format("%-26s %10.2f%n", "GST (5%)",  b.getTaxAmount()));
            sb.append("========================================\n");
            sb.append(String.format("%-26s %10.2f%n", "TOTAL",     b.getTotalAmount()));
            sb.append("========================================\n");
            return sb.toString();
        }
    }

    //ui classes
    // LoginFrame 
    static class LoginFrame extends JFrame {
        private final UserService userService = new UserService();
        private JTextField     usernameField;
        private JPasswordField passwordField;
        private JLabel         errorLabel;

        public LoginFrame() {
            setTitle("Cafeteria System — Login");
            setSize(380, 280); setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null); setResizable(false);
            buildUI();
        }

        private void buildUI() {
            JPanel main = new JPanel(new BorderLayout(10, 10));
            main.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
            main.setBackground(Color.WHITE);

            JLabel title = new JLabel("Cafeteria Login", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 20));
            title.setForeground(new Color(26, 82, 118));
            main.add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridLayout(4, 2, 8, 10));
            form.setBackground(Color.WHITE);
            form.add(new JLabel("Username:")); usernameField = new JTextField();      form.add(usernameField);
            form.add(new JLabel("Password:")); passwordField = new JPasswordField();  form.add(passwordField);
            errorLabel = new JLabel(" ", SwingConstants.CENTER);
            errorLabel.setForeground(Color.RED);
            errorLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            form.add(errorLabel); form.add(new JLabel());

            JButton loginBtn = makeBtn("Login", new Color(26, 82, 118));
            loginBtn.addActionListener(e -> handleLogin());
            form.add(loginBtn);
            passwordField.addActionListener(e -> handleLogin());
            main.add(form, BorderLayout.CENTER);
            add(main);
        }

        private void handleLogin() {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();
            if (user.isEmpty() || pass.isEmpty()) { errorLabel.setText("Enter both fields."); return; }
            try {
                User u = userService.login(user, pass);
                if (u == null) { errorLabel.setText("Invalid username or password."); passwordField.setText(""); return; }
                dispose();
                if (u.isAdmin()) new AdminDashboard(u).setVisible(true);
                else             new OperatorDashboard(u).setVisible(true);
            } catch (Exception ex) { errorLabel.setText("Error: " + ex.getMessage()); }
        }
    }

    // AdminDashboard 
    static class AdminDashboard extends JFrame {
        private final User currentUser;
        public AdminDashboard(User u) {
            currentUser = u;
            setTitle("Admin Dashboard — " + u.getUsername());
            setSize(500, 400); setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null);
            buildUI();
        }
        private void buildUI() {
            JPanel main = new JPanel(new BorderLayout(10,10));
            main.setBorder(BorderFactory.createEmptyBorder(20,30,20,30)); main.setBackground(Color.WHITE);
            JLabel hdr = new JLabel("Welcome, " + currentUser.getUsername() + "  (Admin)", SwingConstants.CENTER);
            hdr.setFont(new Font("Arial", Font.BOLD, 18)); hdr.setForeground(new Color(26,82,118));
            main.add(hdr, BorderLayout.NORTH);
            JPanel btns = new JPanel(new GridLayout(4,1,10,14));
            btns.setBackground(Color.WHITE); btns.setBorder(BorderFactory.createEmptyBorder(20,60,20,60));
            btns.add(makeBtn("Manage Menu Items",   new Color(26,82,118),  e -> open(new MenuManagementPanel())));
            btns.add(makeBtn("View Order History",  new Color(26,82,118),  e -> open(new OrderHistoryPanel())));
            btns.add(makeBtn("Daily Sales Summary", new Color(26,82,118),  e -> open(new SalesSummaryPanel())));
            btns.add(makeBtn("Logout",              new Color(192,57,43),  e -> { dispose(); new LoginFrame().setVisible(true); }));
            main.add(btns, BorderLayout.CENTER); add(main);
        }
        private void open(JPanel panel) {
            JFrame f = new JFrame(); f.setSize(760,560); f.setLocationRelativeTo(this);
            f.setDefaultCloseOperation(DISPOSE_ON_CLOSE); f.add(panel); f.setVisible(true);
        }
        private JButton makeBtn(String text, Color bg, java.awt.event.ActionListener al) {
            JButton b = new JButton(text); b.setFont(new Font("Arial",Font.PLAIN,14));
            b.setBackground(bg); b.setForeground(Color.WHITE); b.setFocusPainted(false);
            b.addActionListener(al); return b;
        }
    }

    // OperatorDashboard
    static class OperatorDashboard extends JFrame {
        private final User currentUser;
        public OperatorDashboard(User u) {
            currentUser = u;
            setTitle("Operator Dashboard — " + u.getUsername());
            setSize(500,360); setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null);
            buildUI();
        }
        private void buildUI() {
            JPanel main = new JPanel(new BorderLayout(10,10));
            main.setBorder(BorderFactory.createEmptyBorder(20,30,20,30)); main.setBackground(Color.WHITE);
            JLabel hdr = new JLabel("Welcome, " + currentUser.getUsername() + "  (Operator)", SwingConstants.CENTER);
            hdr.setFont(new Font("Arial",Font.BOLD,18)); hdr.setForeground(new Color(192,57,43));
            main.add(hdr, BorderLayout.NORTH);
            JPanel btns = new JPanel(new GridLayout(3,1,10,14));
            btns.setBackground(Color.WHITE); btns.setBorder(BorderFactory.createEmptyBorder(20,60,20,60));
            btns.add(makeBtn("Place New Order",    new Color(192,57,43),  e -> open(new PlaceOrderPanel())));
            btns.add(makeBtn("View Order History", new Color(192,57,43),  e -> open(new OrderHistoryPanel())));
            btns.add(makeBtn("Logout",             new Color(100,100,100),e -> { dispose(); new LoginFrame().setVisible(true); }));
            main.add(btns, BorderLayout.CENTER); add(main);
        }
        private void open(JPanel panel) {
            JFrame f = new JFrame(); f.setSize(760,580); f.setLocationRelativeTo(this);
            f.setDefaultCloseOperation(DISPOSE_ON_CLOSE); f.add(panel); f.setVisible(true);
        }
        private JButton makeBtn(String text, Color bg, java.awt.event.ActionListener al) {
            JButton b = new JButton(text); b.setFont(new Font("Arial",Font.PLAIN,14));
            b.setBackground(bg); b.setForeground(Color.WHITE); b.setFocusPainted(false);
            b.addActionListener(al); return b;
        }
    }

    // MenuManagementPanel 
    static class MenuManagementPanel extends JPanel {
        private final MenuService menuService = new MenuService();
        private JTable table; private DefaultTableModel tableModel;
        private JTextField nameField, categoryField, priceField;
        private JCheckBox availableBox;
        private JButton addBtn, updateBtn, deleteBtn, clearBtn;
        private JLabel statusLabel;
        private List<MenuItem> items; private int selectedId = -1;

        public MenuManagementPanel() {
            setLayout(new BorderLayout(10,10));
            setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            setBackground(Color.WHITE); buildUI(); loadTable();
        }

        private void buildUI() {
            JLabel title = new JLabel("Menu Management", SwingConstants.CENTER);
            title.setFont(new Font("Arial",Font.BOLD,16)); title.setForeground(new Color(26,82,118));
            add(title, BorderLayout.NORTH);

            tableModel = new DefaultTableModel(new String[]{"ID","Name","Category","Price","Available"},0) {
                public boolean isCellEditable(int r,int c){return false;}
            };
            table = new JTable(tableModel); table.setRowHeight(24);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) onRowSelected(); });
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(Color.WHITE); form.setBorder(BorderFactory.createTitledBorder("Item Details"));
            GridBagConstraints g = new GridBagConstraints(); g.insets=new Insets(4,6,4,6); g.anchor=GridBagConstraints.WEST;
            nameField=new JTextField(15); categoryField=new JTextField(15); priceField=new JTextField(15);
            availableBox=new JCheckBox("Available",true);
            addRow(form,g,0,"Name:",     nameField);
            addRow(form,g,1,"Category:", categoryField);
            addRow(form,g,2,"Price:",    priceField);
            g.gridx=0;g.gridy=3; form.add(new JLabel("Status:"),g); g.gridx=1; form.add(availableBox,g);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,8,4));
            btnPanel.setBackground(Color.WHITE);
            addBtn    = makeBtn("Add",    new Color(30,130,76));
            updateBtn = makeBtn("Update", new Color(26,82,118));
            deleteBtn = makeBtn("Delete", new Color(192,57,43));
            clearBtn  = makeBtn("Clear",  Color.GRAY);
            updateBtn.setEnabled(false); deleteBtn.setEnabled(false);
            btnPanel.add(addBtn); btnPanel.add(updateBtn); btnPanel.add(deleteBtn); btnPanel.add(clearBtn);
            addBtn.addActionListener(e    -> handleAdd());
            updateBtn.addActionListener(e -> handleUpdate());
            deleteBtn.addActionListener(e -> handleDelete());
            clearBtn.addActionListener(e  -> clearForm());

            statusLabel = new JLabel(" ", SwingConstants.CENTER);
            statusLabel.setFont(new Font("Arial",Font.ITALIC,11));
            JPanel south = new JPanel(new BorderLayout());
            south.setBackground(Color.WHITE);
            south.add(form,BorderLayout.NORTH); south.add(btnPanel,BorderLayout.CENTER); south.add(statusLabel,BorderLayout.SOUTH);
            add(south, BorderLayout.SOUTH);
        }

        private void loadTable() {
            tableModel.setRowCount(0);
            try {
                items = menuService.getAllItems();
                for (MenuItem mi : items)
                    tableModel.addRow(new Object[]{mi.getItemId(),mi.getName(),mi.getCategory(),
                            String.format("%.2f",mi.getPrice()), mi.isAvailable()?"Yes":"No"});
            } catch (Exception ex) { showErr("Load failed: "+ex.getMessage()); }
        }

        private void onRowSelected() {
            int row = table.getSelectedRow(); if (row<0) return;
            MenuItem mi = items.get(row); selectedId = mi.getItemId();
            nameField.setText(mi.getName()); categoryField.setText(mi.getCategory());
            priceField.setText(String.valueOf(mi.getPrice())); availableBox.setSelected(mi.isAvailable());
            updateBtn.setEnabled(true); deleteBtn.setEnabled(true); addBtn.setEnabled(false);
        }

        private void handleAdd() {
            try { menuService.addItem(buildItem(-1)); showOk("Item added."); clearForm(); loadTable(); }
            catch (Exception ex) { showErr(ex.getMessage()); }
        }
        private void handleUpdate() {
            try { menuService.updateItem(buildItem(selectedId)); showOk("Item updated."); clearForm(); loadTable(); }
            catch (Exception ex) { showErr(ex.getMessage()); }
        }
        private void handleDelete() {
            if (JOptionPane.showConfirmDialog(this,"Delete this item?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
            try { menuService.deleteItem(selectedId); showOk("Item deleted."); clearForm(); loadTable(); }
            catch (Exception ex) { showErr(ex.getMessage()); }
        }

        private MenuItem buildItem(int id) {
            String n=nameField.getText().trim(), c=categoryField.getText().trim(), ps=priceField.getText().trim();
            double price;
            try { price=Double.parseDouble(ps); } catch (NumberFormatException e) { throw new IllegalArgumentException("Price must be a number."); }
            return new MenuItem(id,n,c,price,availableBox.isSelected());
        }

        private void clearForm() {
            nameField.setText(""); categoryField.setText(""); priceField.setText("");
            availableBox.setSelected(true); selectedId=-1;
            addBtn.setEnabled(true); updateBtn.setEnabled(false); deleteBtn.setEnabled(false);
            table.clearSelection(); statusLabel.setText(" ");
        }

        private void showOk(String m)  { statusLabel.setForeground(new Color(30,130,76)); statusLabel.setText(m); }
        private void showErr(String m) { statusLabel.setForeground(Color.RED);            statusLabel.setText(m); }
        private JButton makeBtn(String t, Color bg) {
            JButton b=new JButton(t); b.setBackground(bg); b.setForeground(Color.WHITE); b.setFocusPainted(false); return b;
        }
        private void addRow(JPanel p,GridBagConstraints g,int row,String lbl,JComponent field) {
            g.gridx=0;g.gridy=row; p.add(new JLabel(lbl),g); g.gridx=1; p.add(field,g);
        }
    }

    // PlaceOrderPanel 
    static class PlaceOrderPanel extends JPanel {
        private final MenuService    menuService    = new MenuService();
        private final OrderService   orderService   = new OrderService();
        private final BillingService billingService = new BillingService();

        private JTable menuTable, orderTable;
        private DefaultTableModel menuModel, orderModel;
        private JTextField customerName, customerPhone, quantityField;
        private JLabel subtotalLabel, statusLabel;
        private List<MenuItem>  menuItems  = new ArrayList<>();
        private List<OrderItem> orderItems = new ArrayList<>();

        public PlaceOrderPanel() {
            setLayout(new BorderLayout(8,8)); setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            setBackground(Color.WHITE); buildUI(); loadMenu();
        }

        private void buildUI() {
            JLabel title = new JLabel("Place New Order", SwingConstants.CENTER);
            title.setFont(new Font("Arial",Font.BOLD,16)); title.setForeground(new Color(192,57,43));
            add(title, BorderLayout.NORTH);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setDividerLocation(340);

            // LEFT — menu
            JPanel left = new JPanel(new BorderLayout(4,4)); left.setBackground(Color.WHITE);
            left.setBorder(BorderFactory.createTitledBorder("Available Menu"));
            menuModel = new DefaultTableModel(new String[]{"ID","Name","Category","Price"},0){
                public boolean isCellEditable(int r,int c){return false;}
            };
            menuTable = new JTable(menuModel); menuTable.setRowHeight(22);
            menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            left.add(new JScrollPane(menuTable), BorderLayout.CENTER);
            JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4)); addRow.setBackground(Color.WHITE);
            addRow.add(new JLabel("Qty:")); quantityField=new JTextField("1",4); addRow.add(quantityField);
            JButton addBtn = makeBtn("Add →", new Color(30,130,76)); addBtn.addActionListener(e->handleAdd());
            addRow.add(addBtn); left.add(addRow, BorderLayout.SOUTH);

            // RIGHT — order
            JPanel right = new JPanel(new BorderLayout(4,4)); right.setBackground(Color.WHITE);
            right.setBorder(BorderFactory.createTitledBorder("Current Order"));
            orderModel = new DefaultTableModel(new String[]{"Item","Qty","Unit Price","Line Total"},0){
                public boolean isCellEditable(int r,int c){return false;}
            };
            orderTable = new JTable(orderModel); orderTable.setRowHeight(22);
            right.add(new JScrollPane(orderTable), BorderLayout.CENTER);

            JPanel bot = new JPanel(new GridLayout(5,2,6,6)); bot.setBackground(Color.WHITE);
            bot.setBorder(BorderFactory.createEmptyBorder(6,4,4,4));
            customerName=new JTextField(); customerPhone=new JTextField();
            subtotalLabel=new JLabel("Rs. 0.00"); subtotalLabel.setFont(new Font("Arial",Font.BOLD,13));
            bot.add(new JLabel("Customer Name:")); bot.add(customerName);
            bot.add(new JLabel("Phone:"));         bot.add(customerPhone);
            bot.add(new JLabel("Subtotal:"));      bot.add(subtotalLabel);
            JButton remBtn = makeBtn("Remove Selected", new Color(192,57,43)); remBtn.addActionListener(e->handleRemove());
            bot.add(remBtn);
            JButton placeBtn = makeBtn("Place Order & Generate Bill", new Color(26,82,118)); placeBtn.addActionListener(e->handlePlace());
            bot.add(placeBtn);
            right.add(bot, BorderLayout.SOUTH);

            split.setLeftComponent(left); split.setRightComponent(right);
            add(split, BorderLayout.CENTER);

            statusLabel=new JLabel(" ",SwingConstants.CENTER);
            statusLabel.setFont(new Font("Arial",Font.ITALIC,11));
            add(statusLabel, BorderLayout.SOUTH);
        }

        private void loadMenu() {
            menuModel.setRowCount(0);
            try {
                menuItems = menuService.getAvailableItems();
                for (MenuItem mi : menuItems)
                    menuModel.addRow(new Object[]{mi.getItemId(),mi.getName(),mi.getCategory(),String.format("%.2f",mi.getPrice())});
            } catch (Exception ex) { showErr("Load error: "+ex.getMessage()); }
        }

        private void handleAdd() {
            int row = menuTable.getSelectedRow(); if (row<0){showErr("Select an item.");return;}
            int qty; try{qty=Integer.parseInt(quantityField.getText().trim()); if(qty<=0)throw new NumberFormatException();}
            catch(NumberFormatException e){showErr("Quantity must be a positive number.");return;}
            MenuItem mi = menuItems.get(row);
            for (OrderItem oi : orderItems) { if (oi.getMenuItem().getItemId()==mi.getItemId()){oi.setQuantity(oi.getQuantity()+qty);refresh();return;} }
            orderItems.add(new OrderItem(mi,qty)); refresh();
        }

        private void handleRemove() {
            int row=orderTable.getSelectedRow(); if(row<0){showErr("Select item to remove.");return;}
            orderItems.remove(row); refresh();
        }

        private void refresh() {
            orderModel.setRowCount(0); double total=0;
            for (OrderItem oi : orderItems) {
                orderModel.addRow(new Object[]{oi.getMenuItem().getName(),oi.getQuantity(),
                        String.format("%.2f",oi.getUnitPrice()),String.format("%.2f",oi.getLineTotal())});
                total+=oi.getLineTotal();
            }
            subtotalLabel.setText(String.format("Rs. %.2f",total));
        }

        private void handlePlace() {
            String name=customerName.getText().trim(), phone=customerPhone.getText().trim();
            if (name.isEmpty()){showErr("Customer name required.");return;}
            if (orderItems.isEmpty()){showErr("Add at least one item.");return;}
            try {
                Customer c = new Customer(name,phone);
                Order o = new Order(c);
                for (OrderItem oi : orderItems) o.addItem(oi);
                int id = orderService.placeOrder(o); o.setOrderId(id);
                Bill bill = billingService.generateBill(o);
                JFrame bf = new JFrame("Bill #"+bill.getBillId());
                bf.setSize(420,480); bf.setLocationRelativeTo(this);
                bf.add(new BillPanel(bill,billingService)); bf.setVisible(true);
                showOk("Order placed! Bill #"+bill.getBillId()+" generated.");
                orderItems.clear(); refresh(); customerName.setText(""); customerPhone.setText("");
            } catch (Exception ex) { showErr(ex.getMessage()); }
        }

        private void showOk(String m)  { statusLabel.setForeground(new Color(30,130,76)); statusLabel.setText(m); }
        private void showErr(String m) { statusLabel.setForeground(Color.RED);            statusLabel.setText(m); }
        private JButton makeBtn(String t,Color bg){JButton b=new JButton(t);b.setBackground(bg);b.setForeground(Color.WHITE);b.setFocusPainted(false);return b;}
    }

    //  BillPanel 
    static class BillPanel extends JPanel {
        public BillPanel(Bill bill, BillingService bs) {
            setLayout(new BorderLayout(8,8)); setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            setBackground(Color.WHITE);
            JLabel title=new JLabel("Bill Receipt",SwingConstants.CENTER);
            title.setFont(new Font("Arial",Font.BOLD,16)); title.setForeground(new Color(26,82,118));
            add(title, BorderLayout.NORTH);
            JTextArea ta=new JTextArea(bs.formatBillText(bill));
            ta.setFont(new Font("Courier New",Font.PLAIN,13)); ta.setEditable(false);
            ta.setBackground(new Color(245,245,245));
            add(new JScrollPane(ta), BorderLayout.CENTER);
            JButton close=new JButton("Close"); close.setBackground(new Color(100,100,100));
            close.setForeground(Color.WHITE); close.setFocusPainted(false);
            close.addActionListener(e->SwingUtilities.getWindowAncestor(this).dispose());
            JPanel s=new JPanel(); s.setBackground(Color.WHITE); s.add(close);
            add(s, BorderLayout.SOUTH);
        }
    }

    //OrderHistoryPanel
    static class OrderHistoryPanel extends JPanel {
        private final OrderService orderService = new OrderService();
        private JTable table; private DefaultTableModel model;
        private JTextArea detail; private List<Order> orders;

        public OrderHistoryPanel() {
            setLayout(new BorderLayout(8,8)); setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            setBackground(Color.WHITE); buildUI(); load();
        }

        private void buildUI() {
            JLabel title=new JLabel("Order History",SwingConstants.CENTER);
            title.setFont(new Font("Arial",Font.BOLD,16)); title.setForeground(new Color(26,82,118));
            add(title, BorderLayout.NORTH);
            model=new DefaultTableModel(new String[]{"Order ID","Customer","Date/Time","Items","Total","Status"},0){
                public boolean isCellEditable(int r,int c){return false;}
            };
            table=new JTable(model); table.setRowHeight(24);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())showDetail();});
            detail=new JTextArea(8,40); detail.setFont(new Font("Courier New",Font.PLAIN,12));
            detail.setEditable(false); detail.setBackground(new Color(245,245,245));
            JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(table),new JScrollPane(detail));
            split.setDividerLocation(260); add(split, BorderLayout.CENTER);
            JButton refresh=new JButton("Refresh"); refresh.addActionListener(e->load());
            JPanel s=new JPanel(new FlowLayout(FlowLayout.RIGHT)); s.setBackground(Color.WHITE); s.add(refresh);
            add(s, BorderLayout.SOUTH);
        }

        private void load() {
            model.setRowCount(0);
            try {
                orders=orderService.getOrderHistory();
                for (Order o : orders)
                    model.addRow(new Object[]{o.getOrderId(),o.getCustomer().getName(),
                            o.getTimestamp().toString().replace("T"," ").substring(0,19),
                            o.getItems().size(),String.format("%.2f",o.getTotal()),o.getStatus()});
            } catch (Exception ex) { detail.setText("Error: "+ex.getMessage()); }
        }

        private void showDetail() {
            int row=table.getSelectedRow(); if(row<0||orders==null) return;
            Order o=orders.get(row);
            StringBuilder sb=new StringBuilder();
            sb.append(String.format("Order #%d  |  %s  |  %s%n%n",o.getOrderId(),o.getCustomer().getName(),
                    o.getTimestamp().toString().replace("T"," ").substring(0,19)));
            sb.append(String.format("%-22s %5s %10s %12s%n","Item","Qty","Unit Price","Line Total"));
            sb.append("-".repeat(54)).append("\n");
            for (OrderItem oi : o.getItems())
                sb.append(String.format("%-22s %5d %10.2f %12.2f%n",
                        oi.getMenuItem().getName(),oi.getQuantity(),oi.getUnitPrice(),oi.getLineTotal()));
            sb.append("-".repeat(54)).append("\n");
            sb.append(String.format("%-38s %12.2f%n","ORDER TOTAL",o.getTotal()));
            detail.setText(sb.toString());
        }
    }

    // SalesSummaryPanel 
    static class SalesSummaryPanel extends JPanel {
        private final OrderService orderService = new OrderService();
        private JSpinner datePicker; private JLabel totalOrdersLbl, totalRevenueLbl;
        private JTable table; private DefaultTableModel model;

        public SalesSummaryPanel() {
            setLayout(new BorderLayout(8,8)); setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            setBackground(Color.WHITE); buildUI();
        }

        private void buildUI() {
            JLabel title=new JLabel("Daily Sales Summary",SwingConstants.CENTER);
            title.setFont(new Font("Arial",Font.BOLD,16)); title.setForeground(new Color(26,82,118));
            add(title, BorderLayout.NORTH);

            JPanel top=new JPanel(new FlowLayout(FlowLayout.CENTER,10,6)); top.setBackground(Color.WHITE);
            top.add(new JLabel("Select Date:"));
            datePicker=new JSpinner(new SpinnerDateModel());
            datePicker.setEditor(new JSpinner.DateEditor(datePicker,"yyyy-MM-dd"));
            datePicker.setPreferredSize(new Dimension(130,28)); top.add(datePicker);
            JButton loadBtn=new JButton("Load Summary"); loadBtn.setBackground(new Color(26,82,118));
            loadBtn.setForeground(Color.WHITE); loadBtn.setFocusPainted(false);
            loadBtn.addActionListener(e->loadSummary()); top.add(loadBtn);

            JPanel stats=new JPanel(new GridLayout(1,4,12,0)); stats.setBackground(Color.WHITE);
            stats.setBorder(BorderFactory.createEmptyBorder(6,0,6,0));
            totalOrdersLbl  = makeStat("Total Orders","—");
            totalRevenueLbl = makeStat("Total Revenue","—");
            stats.add(totalOrdersLbl); stats.add(totalRevenueLbl);
            stats.add(new JLabel()); stats.add(new JLabel());

            model=new DefaultTableModel(new String[]{"Order ID","Customer","Time","Items","Total (Rs.)"},0){
                public boolean isCellEditable(int r,int c){return false;}
            };
            table=new JTable(model); table.setRowHeight(24);

            JPanel center=new JPanel(new BorderLayout(4,4)); center.setBackground(Color.WHITE);
            center.add(stats,BorderLayout.NORTH); center.add(new JScrollPane(table),BorderLayout.CENTER);

            JPanel main=new JPanel(new BorderLayout(6,6)); main.setBackground(Color.WHITE);
            main.add(top,BorderLayout.NORTH); main.add(center,BorderLayout.CENTER);
            add(main, BorderLayout.CENTER);
        }

        private void loadSummary() {
            java.util.Date sel=(java.util.Date)datePicker.getValue();
            LocalDate date=sel.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            model.setRowCount(0);
            try {
                List<Order> orders=orderService.getDailySummary(date);
                double rev=orderService.getTotalRevenue(orders);
                totalOrdersLbl.setText("<html><b>Total Orders</b><br/><font size=+1>"+orders.size()+"</font></html>");
                totalRevenueLbl.setText("<html><b>Total Revenue</b><br/><font size=+1>Rs."+String.format("%.2f",rev)+"</font></html>");
                for (Order o : orders)
                    model.addRow(new Object[]{o.getOrderId(),o.getCustomer().getName(),
                            o.getTimestamp().toLocalTime().toString().substring(0,8),
                            o.getItems().size(),String.format("%.2f",o.getTotal())});
                if (orders.isEmpty())
                    JOptionPane.showMessageDialog(this,"No orders found for "+date,"No Data",JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
            }
        }

        private JLabel makeStat(String heading, String value) {
            JLabel lbl=new JLabel("<html><b>"+heading+"</b><br/><font size=+1>"+value+"</font></html>");
            lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(26,82,118),1,true),
                BorderFactory.createEmptyBorder(6,10,6,10)));
            lbl.setBackground(new Color(235,245,251)); lbl.setOpaque(true);
            return lbl;
        }
    }

    // Shared helper 
    static JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.PLAIN, 14));
        b.setBackground(bg); b.setForeground(Color.WHITE); b.setFocusPainted(false);
        return b;
    }
}
