# BankEase - Secure Banking Web Application

A comprehensive banking web application built with Spring Boot, featuring user authentication, account management, transactions, and admin panel.

## 🏗️ Architecture

- **Frontend**: HTML5, CSS3, JavaScript, Bootstrap 5, Thymeleaf
- **Backend**: Spring Boot 3.2.0 (MVC + Security + Data JPA)
- **Database**: MySQL 8.0
- **Security**: Spring Security with BCrypt password hashing
- **Build Tool**: Maven

## 🚀 Features

### Core Banking Features
- **User Registration & Authentication**
  - Secure registration with validation
  - Form-based login with CSRF protection
  - Account locking after 3 failed attempts
  - Session management with timeout

- **Account Management**
  - Multiple account types (Savings, Checking, Business)
  - Account approval workflow
  - Real-time balance updates
  - Account status management

- **Transaction System**
  - Deposit and withdrawal operations
  - Transaction history with pagination
  - Real-time transaction processing
  - Transaction validation and security

- **User Dashboard**
  - Account overview and summary
  - Recent transaction history
  - Quick action buttons
  - Responsive design

### Admin Features
- **User Management**
  - View all users
  - Enable/disable user accounts
  - Unlock locked accounts
  - User activity monitoring

- **Account Administration**
  - Approve/reject account applications
  - Suspend/reactivate accounts
  - Account balance monitoring
  - Account type management

- **Transaction Monitoring**
  - View all transactions
  - Transaction status management
  - Financial reporting
  - System statistics

### Security Features
- **Authentication & Authorization**
  - Role-based access control (USER, ADMIN)
  - Session-based authentication
  - CSRF protection
  - SQL injection prevention

- **Password Security**
  - BCrypt password hashing
  - Password strength validation
  - Account lockout mechanism
  - Secure password reset

- **Input Validation**
  - Server-side validation with Bean Validation
  - Client-side JavaScript validation
  - XSS protection
  - Data sanitization

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Web browser (Chrome, Firefox, Safari, Edge)

## 🔧 Setup Instructions

### 1. Database Setup

1. Install MySQL and create a database:
```sql
CREATE DATABASE bankease_db;
CREATE USER 'bankease_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON bankease_db.* TO 'bankease_user'@'localhost';
FLUSH PRIVILEGES;
```

2. Update database configuration in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bankease_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 2. Application Setup

1. Clone or download the project
2. Navigate to the project directory
3. Build the application:
```bash
mvn clean compile
```

4. Run the application:
```bash
mvn spring-boot:run
```

5. Access the application at: `http://localhost:8080`

## 👤 Default Credentials

### Admin Account
- **Username**: `admin`
- **Password**: `admin123`
- **Role**: ADMIN

### Creating User Accounts
1. Visit the registration page
2. Fill out the registration form
3. Login with your credentials
4. Apply for bank accounts (requires admin approval)

## 🎯 Usage Guide

### For Regular Users

1. **Registration**
   - Click "Create Account" on login page
   - Fill out personal information
   - Submit registration form
   - Login with new credentials

2. **Account Management**
   - Navigate to "Open Account"
   - Select account type
   - Wait for admin approval
   - View account status on dashboard

3. **Banking Operations**
   - Use "Deposit Money" for deposits
   - Use "Withdraw Money" for withdrawals
   - View transaction history
   - Check account balances

4. **Profile Management**
   - Update personal information
   - Change password
   - View account summary

### For Administrators

1. **User Management**
   - Access admin dashboard
   - View all registered users
   - Enable/disable accounts
   - Unlock locked accounts

2. **Account Administration**
   - Review pending account applications
   - Approve or reject accounts
   - Monitor account balances
   - Manage account status

3. **System Monitoring**
   - View transaction reports
   - Monitor system statistics
   - Track user activity
   - Generate financial reports

## 🔒 Security Considerations

- All passwords are hashed using BCrypt
- CSRF tokens protect against cross-site request forgery
- Session management prevents unauthorized access
- Input validation prevents malicious data entry
- SQL injection protection through prepared statements
- Account lockout prevents brute force attacks

## 📱 Responsive Design

The application is fully responsive and works on:
- Desktop computers
- Tablets
- Mobile phones
- Various screen sizes and orientations

## 🎨 UI/UX Features

- Modern Bootstrap 5 design
- Smooth animations and transitions
- Interactive elements with hover effects
- Real-time form validation
- Password strength indicator
- Loading states and feedback
- Accessibility support

## 🔧 Development Features

- Hot reload with Spring Boot DevTools
- Comprehensive logging
- Error handling and validation
- Clean code architecture
- Separation of concerns
- RESTful design principles

## 📊 Database Schema

The application uses the following main entities:
- **Users**: User account information and authentication
- **BankAccounts**: Bank account details and balances
- **Transactions**: Transaction history and details

## 🚀 Deployment

For production deployment:

1. Update `application.properties` for production database
2. Configure SSL/HTTPS
3. Set up proper logging
4. Configure external configuration
5. Build WAR file: `mvn clean package`
6. Deploy to application server

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📝 License

This project is for educational purposes. Please ensure compliance with banking regulations if used in production.

## 🆘 Support

For issues or questions:
1. Check the application logs
2. Verify database connectivity
3. Ensure all dependencies are installed
4. Check browser console for JavaScript errors

## 🔄 Future Enhancements

Potential improvements:
- REST API for mobile applications
- Email notifications
- Two-factor authentication
- Advanced reporting and analytics
- Integration with external payment systems
- Automated testing suite
- Docker containerization

---

**BankEase** - Secure Banking Made Easy 🏦
