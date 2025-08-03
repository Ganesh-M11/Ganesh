// Dashboard JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Initialize dashboard features
    initializeAnimations();
    initializeTooltips();
    initializeCharts();
    setupRealTimeUpdates();
    setupInteractiveFeatures();

    // Add animations to cards
    function initializeAnimations() {
        const cards = document.querySelectorAll('.card');
        cards.forEach((card, index) => {
            card.style.animationDelay = `${index * 0.1}s`;
            card.classList.add('fade-in-up');
        });

        // Animate numbers
        animateNumbers();
    }

    // Animate counter numbers
    function animateNumbers() {
        const counters = document.querySelectorAll('[data-animate="counter"]');
        
        counters.forEach(counter => {
            const target = parseInt(counter.textContent.replace(/[^0-9]/g, ''));
            const duration = 2000;
            const step = target / (duration / 16);
            let current = 0;

            const timer = setInterval(() => {
                current += step;
                if (current >= target) {
                    current = target;
                    clearInterval(timer);
                }
                
                // Format number based on original content
                if (counter.textContent.includes('$')) {
                    counter.textContent = '$' + Math.floor(current).toLocaleString();
                } else {
                    counter.textContent = Math.floor(current).toLocaleString();
                }
            }, 16);
        });
    }

    // Initialize tooltips
    function initializeTooltips() {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }

    // Initialize charts (placeholder for future implementation)
    function initializeCharts() {
        // Balance trend chart
        const balanceChartContainer = document.getElementById('balanceChart');
        if (balanceChartContainer) {
            createBalanceChart();
        }

        // Transaction distribution chart
        const transactionChartContainer = document.getElementById('transactionChart');
        if (transactionChartContainer) {
            createTransactionChart();
        }
    }

    // Setup real-time updates
    function setupRealTimeUpdates() {
        // Update account balances every 30 seconds
        setInterval(updateAccountBalances, 30000);
        
        // Update recent transactions every 60 seconds
        setInterval(updateRecentTransactions, 60000);
        
        // Update current time
        updateCurrentTime();
        setInterval(updateCurrentTime, 1000);
    }

    // Setup interactive features
    function setupInteractiveFeatures() {
        // Account card hover effects
        const accountCards = document.querySelectorAll('.account-card');
        accountCards.forEach(card => {
            card.addEventListener('mouseenter', function() {
                this.style.transform = 'translateY(-5px)';
                this.style.boxShadow = '0 10px 25px rgba(0,0,0,0.15)';
            });

            card.addEventListener('mouseleave', function() {
                this.style.transform = 'translateY(0)';
                this.style.boxShadow = '';
            });
        });

        // Quick action buttons
        const quickActionBtns = document.querySelectorAll('.quick-action-btn');
        quickActionBtns.forEach(btn => {
            btn.addEventListener('click', function(e) {
                // Add ripple effect
                createRippleEffect(e, this);
            });
        });

        // Search functionality
        const searchInput = document.getElementById('searchTransactions');
        if (searchInput) {
            searchInput.addEventListener('input', function() {
                filterTransactions(this.value);
            });
        }

        // Refresh buttons
        const refreshBtns = document.querySelectorAll('.refresh-btn');
        refreshBtns.forEach(btn => {
            btn.addEventListener('click', function() {
                refreshSection(this.dataset.section);
            });
        });
    }

    // Update account balances
    function updateAccountBalances() {
        const accountCards = document.querySelectorAll('[data-account-id]');
        
        accountCards.forEach(card => {
            const accountId = card.dataset.accountId;
            
            fetch(`/user/account/${accountId}/balance`)
                .then(response => response.json())
                .then(balance => {
                    const balanceElement = card.querySelector('.account-balance');
                    if (balanceElement) {
                        const oldBalance = parseFloat(balanceElement.textContent.replace(/[^0-9.-]/g, ''));
                        const newBalance = parseFloat(balance);
                        
                        if (oldBalance !== newBalance) {
                            animateBalanceChange(balanceElement, oldBalance, newBalance);
                        }
                    }
                })
                .catch(error => {
                    console.error('Error updating balance:', error);
                });
        });
    }

    // Animate balance changes
    function animateBalanceChange(element, oldValue, newValue) {
        element.classList.add('balance-updating');
        
        const duration = 1000;
        const steps = 60;
        const stepValue = (newValue - oldValue) / steps;
        let currentValue = oldValue;
        let step = 0;

        const animation = setInterval(() => {
            step++;
            currentValue += stepValue;
            
            element.textContent = '$' + currentValue.toFixed(2);
            
            if (step >= steps) {
                clearInterval(animation);
                element.textContent = '$' + newValue.toFixed(2);
                element.classList.remove('balance-updating');
                
                // Add flash effect
                element.classList.add('balance-updated');
                setTimeout(() => {
                    element.classList.remove('balance-updated');
                }, 2000);
            }
        }, duration / steps);
    }

    // Update recent transactions
    function updateRecentTransactions() {
        fetch('/user/transactions/recent')
            .then(response => response.json())
            .then(transactions => {
                updateTransactionsList(transactions);
            })
            .catch(error => {
                console.error('Error updating transactions:', error);
            });
    }

    // Update transactions list
    function updateTransactionsList(transactions) {
        const container = document.querySelector('.recent-transactions-list');
        if (!container) return;

        container.innerHTML = '';
        
        if (transactions.length === 0) {
            container.innerHTML = '<p class="text-muted text-center">No recent transactions</p>';
            return;
        }

        transactions.forEach(transaction => {
            const transactionElement = createTransactionElement(transaction);
            container.appendChild(transactionElement);
        });
    }

    // Create transaction element
    function createTransactionElement(transaction) {
        const div = document.createElement('div');
        div.className = 'd-flex align-items-center mb-3 transaction-item';
        
        const typeClass = transaction.transactionType === 'DEPOSIT' ? 'success' : 'warning';
        const icon = transaction.transactionType === 'DEPOSIT' ? 'arrow-down' : 'arrow-up';
        const sign = transaction.transactionType === 'DEPOSIT' ? '+' : '-';
        
        div.innerHTML = `
            <div class="flex-shrink-0 me-3">
                <div class="rounded-circle d-flex align-items-center justify-content-center bg-${typeClass}-subtle text-${typeClass}" 
                     style="width: 40px; height: 40px;">
                    <i class="fas fa-${icon}"></i>
                </div>
            </div>
            <div class="flex-grow-1">
                <div class="fw-semibold">${transaction.transactionType}</div>
                <small class="text-muted">${formatDate(transaction.createdAt)}</small>
            </div>
            <div class="text-end">
                <div class="fw-bold text-${typeClass}">
                    ${sign}$${parseFloat(transaction.amount).toFixed(2)}
                </div>
            </div>
        `;
        
        return div;
    }

    // Update current time
    function updateCurrentTime() {
        const timeElements = document.querySelectorAll('.current-time');
        const now = new Date();
        const timeString = now.toLocaleTimeString();
        
        timeElements.forEach(element => {
            element.textContent = timeString;
        });
    }

    // Create ripple effect
    function createRippleEffect(event, button) {
        const ripple = document.createElement('span');
        const rect = button.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        const x = event.clientX - rect.left - size / 2;
        const y = event.clientY - rect.top - size / 2;
        
        ripple.style.width = ripple.style.height = size + 'px';
        ripple.style.left = x + 'px';
        ripple.style.top = y + 'px';
        ripple.classList.add('ripple');
        
        button.appendChild(ripple);
        
        setTimeout(() => {
            ripple.remove();
        }, 600);
    }

    // Filter transactions
    function filterTransactions(query) {
        const transactions = document.querySelectorAll('.transaction-item');
        
        transactions.forEach(transaction => {
            const text = transaction.textContent.toLowerCase();
            const matches = text.includes(query.toLowerCase());
            
            transaction.style.display = matches ? 'flex' : 'none';
        });
    }

    // Refresh section
    function refreshSection(section) {
        const refreshBtn = document.querySelector(`[data-section="${section}"]`);
        if (refreshBtn) {
            refreshBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            
            setTimeout(() => {
                refreshBtn.innerHTML = '<i class="fas fa-sync-alt"></i>';
                
                switch (section) {
                    case 'accounts':
                        location.reload(); // Simple refresh for now
                        break;
                    case 'transactions':
                        updateRecentTransactions();
                        break;
                    case 'balance':
                        updateAccountBalances();
                        break;
                }
            }, 1000);
        }
    }

    // Utility functions
    function formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    }

    function formatCurrency(amount) {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount);
    }

    // Placeholder chart functions
    function createBalanceChart() {
        // This would integrate with Chart.js or similar library
        console.log('Balance chart would be initialized here');
    }

    function createTransactionChart() {
        // This would integrate with Chart.js or similar library
        console.log('Transaction chart would be initialized here');
    }

    // Auto-dismiss alerts
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            const closeBtn = alert.querySelector('.btn-close');
            if (closeBtn) {
                closeBtn.click();
            }
        }, 5000);
    });

    // Add keyboard shortcuts
    document.addEventListener('keydown', function(e) {
        // Ctrl/Cmd + D for deposit
        if ((e.ctrlKey || e.metaKey) && e.key === 'd') {
            e.preventDefault();
            const depositBtn = document.querySelector('a[href*="deposit"]');
            if (depositBtn) depositBtn.click();
        }
        
        // Ctrl/Cmd + W for withdraw
        if ((e.ctrlKey || e.metaKey) && e.key === 'w') {
            e.preventDefault();
            const withdrawBtn = document.querySelector('a[href*="withdraw"]');
            if (withdrawBtn) withdrawBtn.click();
        }
        
        // Ctrl/Cmd + T for transactions
        if ((e.ctrlKey || e.metaKey) && e.key === 't') {
            e.preventDefault();
            const transactionsBtn = document.querySelector('a[href*="transactions"]');
            if (transactionsBtn) transactionsBtn.click();
        }
    });

    // Add CSS for animations
    const style = document.createElement('style');
    style.textContent = `
        .balance-updating {
            color: #ffc107 !important;
        }
        
        .balance-updated {
            animation: flash 0.5s ease-in-out;
        }
        
        @keyframes flash {
            0%, 100% { background-color: transparent; }
            50% { background-color: rgba(40, 167, 69, 0.2); }
        }
        
        .ripple {
            position: absolute;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.6);
            transform: scale(0);
            animation: ripple-animation 0.6s linear;
            pointer-events: none;
        }
        
        @keyframes ripple-animation {
            to {
                transform: scale(4);
                opacity: 0;
            }
        }
        
        .transaction-item {
            transition: all 0.3s ease;
        }
        
        .transaction-item:hover {
            background-color: rgba(0, 0, 0, 0.02);
            border-radius: 0.5rem;
            padding: 0.5rem;
            margin: -0.5rem;
        }
    `;
    document.head.appendChild(style);
});