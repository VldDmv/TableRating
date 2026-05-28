/**
 * Header dropdown functionality
 * Standalone script for user menu dropdown
 */

(function () {
    'use strict';

    // Wait for DOM to be ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initHeaderDropdown);
    } else {
        initHeaderDropdown();
    }

    function initHeaderDropdown() {
        const userMenuButton = document.getElementById('user-menu-button');
        const userMenuDropdown = document.getElementById('user-menu-dropdown');

        if (!userMenuButton || !userMenuDropdown) {
            return; // Elements not found
        }

        // Toggle dropdown on button click
        userMenuButton.addEventListener('click', function (e) {
            e.stopPropagation();
            userMenuDropdown.classList.toggle('show');
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', function (e) {
            if (!userMenuButton.contains(e.target) && !userMenuDropdown.contains(e.target)) {
                userMenuDropdown.classList.remove('show');
            }
        });

        // Close dropdown on escape key
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                userMenuDropdown.classList.remove('show');
            }
        });
    }
})();
