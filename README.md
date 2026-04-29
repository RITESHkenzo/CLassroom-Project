GROUP NAME: Code Rangers

Project TITLE: BOOKaTABLE

Project THEME: Cafeteria order and billing system

PROJECT LEAD: 
         
         Ritesh Singh  590017034

MEMBERS: 

         Anurag Kumar  590011480
         
         Bhavishya Pradhan 590013542
         
         Mohit Vats   590016048

PROBLEM STATEMENT: Manual order taking and billing often leads to mistakes in item totals, order tracking, and
daily sales records. The goal is to build a Java desktop system that manages orders and billing
accurately.

Programming Language

         Java — the entire project is built in Java

GUI / Frontend

         Java Swing — used to build all the windows and screens (login, menu, order placement, bill display)

Database Connectivity

         JDBC (Java Database Connectivity) — used to connect Java code to the database and run SQL queries

Database

         MySQL — stores all data like menu items, orders, bills, and users

How to Run the Project
1.	Step 1: Run schema.sql in MySQL Workbench to create cafeteria_db and all tables.
2.	Step 2: Update DBConnection.java — set DB_USER and DB_PASS to your MySQL credentials.
3.	Step 3: Add mysql-connector-j JAR to your project build path in IntelliJ or Eclipse.
4.	Step 4: Right-click Main.java and select Run. The Login screen will appear.
5.	Step 5: Login with username: admin  /  password: admin123 (Admin) OR  username: operator  /  password: operator123 (Operator).
6.	Step 6: Test the full workflow: Admin adds menu item → Operator places order → Bill is generated → Admin views daily summary.

How to run java file:
javac -cp .;mysql-connector-j.jar CafeteriaSystem.java
java  -cp .;mysql-connector-j.jar CafeteriaSystem
