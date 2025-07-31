// Registration Page JavaScript
document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.getElementById('registerForm');
    const usernameInput = document.getElementById('username');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const togglePasswordBtn = document.getElementById('togglePassword');
    const toggleConfirmPasswordBtn = document.getElementById('toggleConfirmPassword');
    const passwordStrengthBar = document.getElementById('passwordStrength');
    const passwordStrengthText = document.getElementById('passwordStrengthText');
    const submitBtn = document.getElementById('registerBtn');

    // Password toggle functionality
    if (togglePasswordBtn) {
        togglePasswordBtn.addEventListener('click', function() {
            togglePasswordVisibility(passwordInput, this);
        });
    }

    if (toggleConfirmPasswordBtn) {
        toggleConfirmPasswordBtn.addEventListener('click', function() {
            togglePasswordVisibility(confirmPasswordInput, this);
        });
    }

    // Real-time validation
    usernameInput.addEventListener('blur', function() {
        validateUsername();
    });

    emailInput.addEventListener('blur', function() {
        validateEmail();
    });

    passwordInput.addEventListener('input', function() {
        checkPasswordStrength();
        if (confirmPasswordInput.value) {
            validatePasswordMatch();
        }
    });

    confirmPasswordInput.addEventListener('input', function() {
        validatePasswordMatch();
    });

    // Form submission
    registerForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        if (validateForm()) {
            showLoadingState();
            // Allow form to submit
            this.submit();
        }
    });

    // Functions
    function togglePasswordVisibility(input, button) {
        const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
        input.setAttribute('type', type);
        
        const icon = button.querySelector('i');
        if (type === 'password') {
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
        } else {
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        }
    }

    function validateUsername() {
        const value = usernameInput.value.trim();
        
        if (!value) {
            showFieldError(usernameInput, 'Username is required');
            return false;
        }
        
        if (value.length < 3 || value.length > 50) {
            showFieldError(usernameInput, 'Username must be 3-50 characters long');
            return false;
        }
        
        // Check username availability via AJAX
        checkUsernameAvailability(value);
        return true;
    }

    function validateEmail() {
        const value = emailInput.value.trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        
        if (!value) {
            showFieldError(emailInput, 'Email is required');
            return false;
        }
        
        if (!emailRegex.test(value)) {
            showFieldError(emailInput, 'Please enter a valid email address');
            return false;
        }
        
        // Check email availability via AJAX
        checkEmailAvailability(value);
        return true;
    }

    function checkPasswordStrength() {
        const password = passwordInput.value;
        const strength = calculatePasswordStrength(password);
        
        updatePasswordStrengthUI(strength);
        
        if (password.length > 0 && password.length < 6) {
            showFieldError(passwordInput, 'Password must be at least 6 characters long');
            return false;
        } else if (password.length >= 6) {
            showFieldSuccess(passwordInput);
            return true;
        }
        
        return password.length === 0;
    }

    function validatePasswordMatch() {
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        
        if (!confirmPassword) {
            clearFieldError(confirmPasswordInput);
            return false;
        }
        
        if (password !== confirmPassword) {
            showFieldError(confirmPasswordInput, 'Passwords do not match');
            return false;
        } else {
            showFieldSuccess(confirmPasswordInput);
            return true;
        }
    }

    function calculatePasswordStrength(password) {
        let strength = 0;
        let feedback = [];
        
        if (password.length >= 6) strength += 20;
        if (password.length >= 8) strength += 10;
        if (/[a-z]/.test(password)) strength += 15;
        if (/[A-Z]/.test(password)) strength += 15;
        if (/[0-9]/.test(password)) strength += 15;
        if (/[^A-Za-z0-9]/.test(password)) strength += 25;
        
        return Math.min(strength, 100);
    }

    function updatePasswordStrengthUI(strength) {
        const password = passwordInput.value;
        
        if (password.length === 0) {
            passwordStrengthBar.style.width = '0%';
            passwordStrengthBar.className = 'progress-bar';
            passwordStrengthText.textContent = 'Enter a password';
            return;
        }
        
        passwordStrengthBar.style.width = strength + '%';
        
        if (strength < 30) {
            passwordStrengthBar.className = 'progress-bar bg-danger';
            passwordStrengthText.textContent = 'Weak password';
        } else if (strength < 60) {
            passwordStrengthBar.className = 'progress-bar bg-warning';
            passwordStrengthText.textContent = 'Fair password';
        } else if (strength < 80) {
            passwordStrengthBar.className = 'progress-bar bg-info';
            passwordStrengthText.textContent = 'Good password';
        } else {
            passwordStrengthBar.className = 'progress-bar bg-success';
            passwordStrengthText.textContent = 'Strong password';
        }
    }

    function checkUsernameAvailability(username) {
        if (username.length < 3) return;
        
        fetch(`/check-username?username=${encodeURIComponent(username)}`)
            .then(response => response.json())
            .then(available => {
                if (available) {
                    showFieldSuccess(usernameInput);
                } else {
                    showFieldError(usernameInput, 'Username is already taken');
                }
            })
            .catch(error => {
                console.error('Error checking username availability:', error);
            });
    }

    function checkEmailAvailability(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) return;
        
        fetch(`/check-email?email=${encodeURIComponent(email)}`)
            .then(response => response.json())
            .then(available => {
                if (available) {
                    showFieldSuccess(emailInput);
                } else {
                    showFieldError(emailInput, 'Email is already registered');
                }
            })
            .catch(error => {
                console.error('Error checking email availability:', error);
            });
    }

    function validateForm() {
        let isValid = true;
        
        // Validate all fields
        const fields = [
            { input: document.getElementById('firstName'), validator: () => document.getElementById('firstName').value.trim() !== '' },
            { input: document.getElementById('lastName'), validator: () => document.getElementById('lastName').value.trim() !== '' },
            { input: usernameInput, validator: () => usernameInput.value.trim().length >= 3 && !usernameInput.classList.contains('is-invalid') },
            { input: emailInput, validator: () => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailInput.value) && !emailInput.classList.contains('is-invalid') },
            { input: document.getElementById('phoneNumber'), validator: () => document.getElementById('phoneNumber').value.trim() !== '' },
            { input: document.getElementById('address'), validator: () => document.getElementById('address').value.trim() !== '' },
            { input: passwordInput, validator: () => passwordInput.value.length >= 6 },
            { input: confirmPasswordInput, validator: () => confirmPasswordInput.value === passwordInput.value },
            { input: document.getElementById('termsCheck'), validator: () => document.getElementById('termsCheck').checked }
        ];
        
        fields.forEach(field => {
            if (!field.validator()) {
                field.input.classList.add('is-invalid');
                isValid = false;
            } else {
                field.input.classList.remove('is-invalid');
                field.input.classList.add('is-valid');
            }
        });
        
        return isValid;
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

    function showLoadingState() {
        submitBtn.disabled = true;
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Creating Account...';
        
        // Re-enable button after 15 seconds as fallback
        setTimeout(() => {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }, 15000);
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

    // Add fade-in animation to the card
    const card = document.querySelector('.card');
    if (card) {
        card.classList.add('fade-in-up');
    }

    // Phone number formatting
    const phoneInput = document.getElementById('phoneNumber');
    phoneInput.addEventListener('input', function() {
        let value = this.value.replace(/\D/g, '');
        if (value.length >= 10) {
            value = value.replace(/(\d{3})(\d{3})(\d{4})/, '($1) $2-$3');
        }
        this.value = value;
    });
});