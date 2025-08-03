# Secure Bank Web Application

A modern, secure, and responsive banking web application built with Spring Boot, MySQL, and HTML/CSS/JavaScript.

## 🏦 Features

### Core Banking Features
- **User Registration & Authentication** - Secure user account creation and login with JWT tokens
- **Account Dashboard** - Comprehensive overview of account balance, recent transactions, and statistics
- **Financial Transactions** - Deposit, withdrawal, and transfer capabilities with real-time processing
- **Transaction History** - Detailed transaction records with filtering and search functionality
- **Notifications System** - Real-time alerts for transactions, security events, and account updates
- **Support System** - Integrated help desk with ticket creation and tracking

### Security Features
- **JWT Authentication** - Secure token-based authentication system
- **Password Encryption** - BCrypt password hashing for enhanced security
- **Account Lockout** - Automatic account locking after failed login attempts
- **Session Management** - Secure session handling with automatic timeouts
- **Input Validation** - Comprehensive server and client-side validation
- **CSRF Protection** - Built-in Cross-Site Request Forgery protection

### Administrative Features
- **Admin Dashboard** - System overview with key metrics and statistics
- **User Management** - Admin tools for managing user accounts and permissions
- **Transaction Monitoring** - Real-time transaction monitoring and reporting
- **Support Ticket Management** - Admin interface for handling customer support requests
- **System Reports** - Comprehensive reporting and analytics

### Technical Features
- **Responsive Design** - Mobile-first responsive design that works on all devices
- **Modern UI/UX** - Clean, intuitive interface with Bootstrap 5 and custom CSS
- **RESTful APIs** - Well-structured REST endpoints for all operations
- **Database Integration** - MySQL database with JPA/Hibernate ORM
- **Email Notifications** - Configurable email system for important alerts
- **Real-time Updates** - Auto-refreshing components for live data updates

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.2.1** - Main application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database abstraction layer
- **MySQL 8.0** - Primary database
- **JWT (JSON Web Tokens)** - Stateless authentication
- **Maven** - Build and dependency management

### Frontend
- **HTML5** - Semantic markup
- **CSS3** - Modern styling with custom properties and animations
- **JavaScript (ES6+)** - Client-side functionality
- **Bootstrap 5** - Responsive UI framework
- **Font Awesome** - Icon library
- **Thymeleaf** - Server-side templating

### Development Tools
- **Spring Boot DevTools** - Hot reloading during development
- **Maven** - Build automation and dependency management
- **Git** - Version control

## 📋 Prerequisites

Before running this application, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6+**
- **MySQL 8.0+**
- **Git** (for cloning the repository)

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd secure-bank
```

### 2. Database Setup
1. Install and start MySQL server
2. Create a new database:
```sql
CREATE DATABASE secure_bank_db;
```

3. Create a MySQL user (optional but recommended):
```sql
CREATE USER 'bankapp'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON secure_bank_db.* TO 'bankapp'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configure Application Properties
Edit `src/main/resources/application.properties` and update the database configuration:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/secure_bank_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=bankapp
spring.datasource.password=secure_password

# Email Configuration (Optional)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### 4. Build and Run the Application
```bash
# Build the application
mvn clean compile

# Run the application
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

### 5. Initial Setup
1. Navigate to `http://localhost:8080`
2. Click "Register" to create a new user account
3. Fill in the registration form with your details
4. Login with your credentials
5. Start using the banking features!

## 📱 Usage

### User Registration
1. Visit the registration page
2. Fill in personal information (name, email, phone)
3. Create a secure password
4. Accept terms and conditions
5. Submit to create your account

### Dashboard Overview
- View current account balance
- See recent transactions
- Check unread notifications
- Access quick actions (deposit, withdraw, transfer)
- Monitor daily transfer limits

### Making Transactions
- **Deposits**: Add funds to your account
- **Withdrawals**: Remove funds from your account
- **Transfers**: Send money to other accounts using account numbers

### Viewing Transaction History
- Filter by transaction type, date range, or amount
- Search through transaction descriptions
- Export transaction data
- View detailed transaction information

### Support System
- Create support tickets for issues or questions
- Track ticket status and responses
- Browse FAQ section
- Contact customer support

## 🔧 Configuration

### Security Configuration
The application includes several security configurations:

- **JWT Token Expiration**: 24 hours (configurable in `application.properties`)
- **Account Lockout**: 5 failed attempts, 30-minute lockout
- **Password Requirements**: Minimum 6 characters
- **Daily Transfer Limit**: $10,000 (configurable in code)

### Email Configuration
Configure SMTP settings in `application.properties` for email notifications:
```properties
spring.mail.host=your-smtp-server
spring.mail.port=587
spring.mail.username=your-email
spring.mail.password=your-password
```

## 🗄️ Database Schema

The application automatically creates the following main tables:
- `users` - User account information
- `transactions` - All financial transactions
- `notifications` - User notifications
- `support_tickets` - Customer support tickets

## 🔐 Security Considerations

- All passwords are encrypted using BCrypt
- JWT tokens are used for stateless authentication
- Input validation on both client and server side
- SQL injection protection through JPA/Hibernate
- CSRF protection enabled
- Secure session management
- Account lockout mechanism for failed login attempts

## 🚀 Deployment

### Production Deployment
1. Update `application.properties` for production environment
2. Configure production database settings
3. Set up SSL/TLS certificates
4. Configure reverse proxy (nginx/Apache)
5. Set up monitoring and logging
6. Configure backup strategies

### Environment Variables
For production deployment, use environment variables instead of hardcoded values:
```bash
export DB_HOST=your-db-host
export DB_USERNAME=your-db-username
export DB_PASSWORD=your-db-password
export JWT_SECRET=your-jwt-secret
```

## 🧪 Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn test jacoco:report
```

### Test Data
The application includes test data initialization for development:
- Default admin user: admin@securebank.com / admin123
- Sample user accounts and transactions

## 📚 API Documentation

### Authentication Endpoints
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/validate-token` - Token validation
- `POST /api/auth/refresh-token` - Token refresh

### Transaction Endpoints
- `GET /api/transactions/history` - Get transaction history
- `POST /api/transactions/deposit` - Make a deposit
- `POST /api/transactions/withdraw` - Make a withdrawal
- `POST /api/transactions/transfer` - Transfer funds

### Notification Endpoints
- `GET /api/notifications` - Get user notifications
- `GET /api/notifications/unread-count` - Get unread count
- `POST /api/notifications/{id}/mark-read` - Mark as read

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

If you encounter any issues or have questions:

1. Check the [Issues](../../issues) page for existing solutions
2. Create a new issue with detailed information
3. Contact the development team

## 🔄 Version History

- **v1.0.0** - Initial release with core banking features
- **v1.1.0** - Added notification system and improved UI
- **v1.2.0** - Enhanced security features and admin panel

## 🙏 Acknowledgments

- Spring Boot community for the excellent framework
- Bootstrap team for the responsive UI components
- Font Awesome for the comprehensive icon library
- MySQL team for the reliable database system

---

**Secure Bank** - Your trusted financial partner 🏦
