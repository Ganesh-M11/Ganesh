// Login Page JavaScript
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const togglePasswordBtn = document.getElementById('togglePassword');
    const submitBtn = loginForm.querySelector('button[type="submit"]');

    // Password toggle functionality
    if (togglePasswordBtn) {
        togglePasswordBtn.addEventListener('click', function() {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            
            const icon = this.querySelector('i');
            if (type === 'password') {
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
            } else {
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
            }
        });
    }

    // Form validation
    loginForm.addEventListener('submit', function(e) {
        let isValid = true;
        
        // Clear previous validation states
        clearValidationStates();
        
        // Validate username
        if (!usernameInput.value.trim()) {
            showFieldError(usernameInput, 'Username is required');
            isValid = false;
        } else if (usernameInput.value.trim().length < 3) {
            showFieldError(usernameInput, 'Username must be at least 3 characters');
            isValid = false;
        }
        
        // Validate password
        if (!passwordInput.value) {
            showFieldError(passwordInput, 'Password is required');
            isValid = false;
        } else if (passwordInput.value.length < 6) {
            showFieldError(passwordInput, 'Password must be at least 6 characters');
            isValid = false;
        }
        
        if (!isValid) {
            e.preventDefault();
            return false;
        }
        
        // Show loading state
        showLoadingState();
    });

    // Real-time validation
    usernameInput.addEventListener('blur', function() {
        validateUsername();
    });

    passwordInput.addEventListener('blur', function() {
        validatePassword();
    });

    // Input event listeners for clearing errors
    usernameInput.addEventListener('input', function() {
        if (this.classList.contains('is-invalid')) {
            clearFieldError(this);
        }
    });

    passwordInput.addEventListener('input', function() {
        if (this.classList.contains('is-invalid')) {
            clearFieldError(this);
        }
    });

    // Functions
    function validateUsername() {
        const value = usernameInput.value.trim();
        if (!value) {
            showFieldError(usernameInput, 'Username is required');
            return false;
        } else if (value.length < 3) {
            showFieldError(usernameInput, 'Username must be at least 3 characters');
            return false;
        } else {
            showFieldSuccess(usernameInput);
            return true;
        }
    }

    function validatePassword() {
        const value = passwordInput.value;
        if (!value) {
            showFieldError(passwordInput, 'Password is required');
            return false;
        } else if (value.length < 6) {
            showFieldError(passwordInput, 'Password must be at least 6 characters');
            return false;
        } else {
            showFieldSuccess(passwordInput);
            return true;
        }
    }

    function showFieldError(field, message) {
        field.classList.remove('is-valid');
        field.classList.add('is-invalid');
        
        const feedback = field.parentNode.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.textContent = message;
        }
    }

    function showFieldSuccess(field) {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');
    }

    function clearFieldError(field) {
        field.classList.remove('is-invalid', 'is-valid');
    }

    function clearValidationStates() {
        const fields = [usernameInput, passwordInput];
        fields.forEach(field => {
            field.classList.remove('is-invalid', 'is-valid');
        });
    }

    function showLoadingState() {
        submitBtn.disabled = true;
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Signing In...';
        
        // Re-enable button after 10 seconds as fallback
        setTimeout(() => {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }, 10000);
    }

    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            if (alert && alert.parentNode) {
                alert.classList.remove('show');
                setTimeout(() => {
                    if (alert.parentNode) {
                        alert.parentNode.removeChild(alert);
                    }
                }, 150);
            }
        }, 5000);
    });

    // Demo credentials click handlers
    const demoCredentials = document.querySelectorAll('.card .text-muted');
    demoCredentials.forEach(demo => {
        demo.addEventListener('click', function() {
            const text = this.textContent;
            if (text.includes('admin')) {
                usernameInput.value = 'admin';
                passwordInput.value = 'admin123';
                usernameInput.focus();
            }
        });
    });

    // Security warning for caps lock
    document.addEventListener('keydown', function(e) {
        if (e.getModifierState && e.getModifierState('CapsLock')) {
            showCapsLockWarning();
        }
    });

    document.addEventListener('keyup', function(e) {
        if (e.getModifierState && !e.getModifierState('CapsLock')) {
            hideCapsLockWarning();
        }
    });

    function showCapsLockWarning() {
        let warning = document.getElementById('capsLockWarning');
        if (!warning) {
            warning = document.createElement('div');
            warning.id = 'capsLockWarning';
            warning.className = 'alert alert-warning mt-2';
            warning.innerHTML = '<i class="fas fa-exclamation-triangle me-2"></i>Caps Lock is ON';
            passwordInput.parentNode.appendChild(warning);
        }
    }

    function hideCapsLockWarning() {
        const warning = document.getElementById('capsLockWarning');
        if (warning) {
            warning.remove();
        }
    }

    // Prevent multiple form submissions
    let isSubmitting = false;
    loginForm.addEventListener('submit', function(e) {
        if (isSubmitting) {
            e.preventDefault();
            return false;
        }
        isSubmitting = true;
    });

    // Add fade-in animation to the card
    const card = document.querySelector('.card');
    if (card) {
        card.classList.add('fade-in-up');
    }
});