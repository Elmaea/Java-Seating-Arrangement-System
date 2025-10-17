document.addEventListener('DOMContentLoaded', function() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function() {
            // Clear any session storage or local storage if used
            sessionStorage.clear();
            localStorage.clear();
            
            // Redirect to login page
            window.location.href = '/login.html';
        });
    }
});