// Admin Custom JavaScript

// Auto-dismiss alerts after 5 seconds
document.addEventListener('DOMContentLoaded', function() {
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});

// Confirm delete actions
function confirmDelete(message) {
    return confirm(message || 'Czy na pewno chcesz usunąć ten element?');
}

// Form validation helper
function validateDateRange(startId, endId) {
    const startTime = document.getElementById(startId);
    const endTime = document.getElementById(endId);

    if (startTime && endTime) {
        endTime.addEventListener('change', function() {
            if (new Date(startTime.value) >= new Date(endTime.value)) {
                alert('Data zakończenia musi być późniejsza niż data rozpoczęcia');
                endTime.value = '';
            }
        });
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Validate datetime inputs
    validateDateRange('startTime', 'endTime');
});
