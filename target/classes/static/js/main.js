/**
 * Main JavaScript file for Secure Bank Application
 * Contains common functionality used across the application
 */

// Global variables
const SecureBank = {
    config: {
        apiBaseUrl: '/api',
        refreshInterval: 30000, // 30 seconds
        dateFormat: 'MMM DD, YYYY HH:mm'
    },
    
    // Authentication token management
    auth: {
        getToken: function() {
            return localStorage.getItem('authToken');
        },
        
        setToken: function(token) {
            localStorage.setItem('authToken', token);
        },
        
        removeToken: function() {
            localStorage.removeItem('authToken');
        },
        
        isAuthenticated: function() {
            const token = this.getToken();
            return token && token !== 'null' && token !== 'undefined';
        }
    },
    
    // API utilities
    api: {
        request: function(url, options = {}) {
            const defaultOptions = {
                headers: {
                    'Content-Type': 'application/json'
                }
            };
            
            // Add auth token if available
            const token = SecureBank.auth.getToken();
            if (token) {
                defaultOptions.headers['Authorization'] = `Bearer ${token}`;
            }
            
            const finalOptions = Object.assign({}, defaultOptions, options);
            
            return fetch(url, finalOptions)
                .then(response => {
                    if (response.status === 401) {
                        SecureBank.auth.removeToken();
                        window.location.href = '/login';
                        return;
                    }
                    return response.json();
                })
                .catch(error => {
                    console.error('API request failed:', error);
                    throw error;
                });
        },
        
        get: function(url) {
            return this.request(url, { method: 'GET' });
        },
        
        post: function(url, data) {
            return this.request(url, {
                method: 'POST',
                body: JSON.stringify(data)
            });
        },
        
        put: function(url, data) {
            return this.request(url, {
                method: 'PUT',
                body: JSON.stringify(data)
            });
        },
        
        delete: function(url) {
            return this.request(url, { method: 'DELETE' });
        }
    },
    
    // UI utilities
    ui: {
        showAlert: function(message, type = 'info', duration = 5000) {
            const alertHtml = `
                <div class="alert alert-${type} alert-dismissible fade show" role="alert">
                    <i class="fas fa-${this.getAlertIcon(type)}"></i> ${message}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            `;
            
            const container = document.querySelector('.container-fluid') || document.querySelector('.container');
            if (container) {
                const alertDiv = document.createElement('div');
                alertDiv.innerHTML = alertHtml;
                container.insertBefore(alertDiv, container.firstChild);
                
                // Auto-dismiss after duration
                if (duration > 0) {
                    setTimeout(() => {
                        const alert = alertDiv.querySelector('.alert');
                        if (alert) {
                            const bsAlert = new bootstrap.Alert(alert);
                            bsAlert.close();
                        }
                    }, duration);
                }
            }
        },
        
        getAlertIcon: function(type) {
            const icons = {
                'success': 'check-circle',
                'danger': 'exclamation-circle',
                'warning': 'exclamation-triangle',
                'info': 'info-circle'
            };
            return icons[type] || 'info-circle';
        },
        
        showLoading: function(element, text = 'Loading...') {
            if (typeof element === 'string') {
                element = document.querySelector(element);
            }
            
            if (element) {
                element.innerHTML = `
                    <i class="fas fa-spinner fa-spin"></i> ${text}
                `;
                element.disabled = true;
            }
        },
        
        hideLoading: function(element, originalText = 'Submit') {
            if (typeof element === 'string') {
                element = document.querySelector(element);
            }
            
            if (element) {
                element.innerHTML = originalText;
                element.disabled = false;
            }
        },
        
        formatCurrency: function(amount, currency = 'USD') {
            return new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency: currency
            }).format(amount);
        },
        
        formatDate: function(date, format = SecureBank.config.dateFormat) {
            if (!(date instanceof Date)) {
                date = new Date(date);
            }
            
            return date.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        }
    },
    
    // Form validation utilities
    validation: {
        email: function(email) {
            const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            return regex.test(email);
        },
        
        phone: function(phone) {
            const regex = /^\(\d{3}\) \d{3}-\d{4}$/;
            return regex.test(phone);
        },
        
        currency: function(amount) {
            const regex = /^\d+(\.\d{1,2})?$/;
            return regex.test(amount) && parseFloat(amount) > 0;
        },
        
        required: function(value) {
            return value && value.trim().length > 0;
        },
        
        minLength: function(value, min) {
            return value && value.length >= min;
        },
        
        maxLength: function(value, max) {
            return value && value.length <= max;
        }
    }
};

// DOM Ready functionality
document.addEventListener('DOMContentLoaded', function() {
    // Initialize tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function(tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Initialize popovers
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function(popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
    
    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    alerts.forEach(alert => {
        setTimeout(() => {
            if (alert && alert.parentNode) {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            }
        }, 5000);
    });
    
    // Add animation classes to cards
    const cards = document.querySelectorAll('.card');
    cards.forEach((card, index) => {
        setTimeout(() => {
            card.classList.add('animate-fade-in-up');
        }, index * 100);
    });
    
    // Form validation setup
    const forms = document.querySelectorAll('.needs-validation');
    Array.from(forms).forEach(form => {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        });
    });
    
    // Phone number formatting
    const phoneInputs = document.querySelectorAll('input[type="tel"]');
    phoneInputs.forEach(input => {
        input.addEventListener('input', function(e) {
            let value = e.target.value.replace(/\D/g, '');
            if (value.length >= 6) {
                value = value.replace(/(\d{3})(\d{3})(\d{4})/, '($1) $2-$3');
            } else if (value.length >= 3) {
                value = value.replace(/(\d{3})(\d{0,3})/, '($1) $2');
            }
            e.target.value = value;
        });
    });
    
    // Currency formatting for amount inputs
    const amountInputs = document.querySelectorAll('input[data-currency="true"]');
    amountInputs.forEach(input => {
        input.addEventListener('blur', function(e) {
            const value = parseFloat(e.target.value);
            if (!isNaN(value)) {
                e.target.value = value.toFixed(2);
            }
        });
    });
    
    // Smooth scrolling for anchor links
    const anchorLinks = document.querySelectorAll('a[href^="#"]');
    anchorLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
    
    // Auto-refresh notification count
    if (SecureBank.auth.isAuthenticated()) {
        setInterval(function() {
            SecureBank.api.get('/api/notifications/unread-count')
                .then(data => {
                    const badge = document.querySelector('.navbar .badge');
                    if (badge) {
                        if (data.unreadCount > 0) {
                            badge.textContent = data.unreadCount;
                            badge.style.display = 'inline';
                        } else {
                            badge.style.display = 'none';
                        }
                    }
                })
                .catch(error => {
                    console.log('Failed to fetch notification count:', error);
                });
        }, SecureBank.config.refreshInterval);
    }
    
    // Handle logout
    const logoutLinks = document.querySelectorAll('a[href="/logout"]');
    logoutLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            SecureBank.auth.removeToken();
            window.location.href = '/login';
        });
    });
    
    // Back to top button
    const backToTopButton = document.createElement('button');
    backToTopButton.innerHTML = '<i class="fas fa-chevron-up"></i>';
    backToTopButton.className = 'btn btn-primary btn-floating position-fixed';
    backToTopButton.style.cssText = `
        bottom: 20px;
        right: 20px;
        border-radius: 50%;
        width: 50px;
        height: 50px;
        display: none;
        z-index: 1000;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    `;
    
    document.body.appendChild(backToTopButton);
    
    // Show/hide back to top button
    window.addEventListener('scroll', function() {
        if (window.pageYOffset > 300) {
            backToTopButton.style.display = 'block';
        } else {
            backToTopButton.style.display = 'none';
        }
    });
    
    // Back to top functionality
    backToTopButton.addEventListener('click', function() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
});

// Utility functions for specific pages
const PageUtils = {
    // Dashboard utilities
    dashboard: {
        updateBalance: function(newBalance) {
            const balanceElements = document.querySelectorAll('[data-balance]');
            balanceElements.forEach(element => {
                element.textContent = SecureBank.ui.formatCurrency(newBalance);
            });
        },
        
        addTransactionRow: function(transaction) {
            const tbody = document.querySelector('.transaction-table tbody');
            if (tbody) {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${SecureBank.ui.formatDate(transaction.createdAt)}</td>
                    <td><span class="badge bg-${this.getTransactionTypeColor(transaction.transactionType)}">${transaction.transactionType}</span></td>
                    <td>${transaction.description}</td>
                    <td class="text-end ${transaction.amount >= 0 ? 'text-success' : 'text-danger'}">
                        ${transaction.amount >= 0 ? '+' : ''}${SecureBank.ui.formatCurrency(Math.abs(transaction.amount))}
                    </td>
                    <td><span class="badge bg-${this.getStatusColor(transaction.status)}">${transaction.status}</span></td>
                `;
                tbody.insertBefore(row, tbody.firstChild);
            }
        },
        
        getTransactionTypeColor: function(type) {
            const colors = {
                'DEPOSIT': 'success',
                'WITHDRAWAL': 'danger',
                'TRANSFER_OUT': 'primary',
                'TRANSFER_IN': 'info'
            };
            return colors[type] || 'secondary';
        },
        
        getStatusColor: function(status) {
            const colors = {
                'COMPLETED': 'success',
                'PENDING': 'warning',
                'FAILED': 'danger'
            };
            return colors[status] || 'secondary';
        }
    },
    
    // Transaction utilities
    transactions: {
        validateAmount: function(amount, maxAmount = null) {
            if (!SecureBank.validation.currency(amount)) {
                return 'Please enter a valid amount';
            }
            
            const numAmount = parseFloat(amount);
            if (numAmount <= 0) {
                return 'Amount must be greater than zero';
            }
            
            if (maxAmount && numAmount > maxAmount) {
                return `Amount cannot exceed ${SecureBank.ui.formatCurrency(maxAmount)}`;
            }
            
            return null;
        },
        
        validateAccountNumber: function(accountNumber) {
            if (!accountNumber || accountNumber.trim().length === 0) {
                return 'Account number is required';
            }
            
            if (!/^ACC\d{9}$/.test(accountNumber)) {
                return 'Invalid account number format';
            }
            
            return null;
        }
    }
};

// Export for use in other scripts
window.SecureBank = SecureBank;
window.PageUtils = PageUtils;