document.addEventListener('DOMContentLoaded', function() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function() {
            // Clear any session storage or local storage if used
            sessionStorage.clear();
            localStorage.clear();
            
            // Redirect to index page
            window.location.href = '/index.html';
        });
    }
});