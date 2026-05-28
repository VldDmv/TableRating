/**
 * Admin Dashboard JavaScript
 */

class Dashboard {
    constructor() {
        this.charts = {};
        this.init();
    }

    init() {
        this.animateStatCards();
        this.initCharts();
    }

    animateStatCards() {
        const cards = document.querySelectorAll('.dashboard-stat-card');

        cards.forEach((card, index) => {
            // Stagger animation
            setTimeout(() => {
                card.style.opacity = '0';
                card.style.transform = 'translateY(20px)';

                requestAnimationFrame(() => {
                    card.style.transition = 'all 0.5s ease-out';
                    card.style.opacity = '1';
                    card.style.transform = 'translateY(0)';
                });

                // Animate number counting
                const valueElement = card.querySelector('.stat-value');
                if (valueElement) {
                    const finalValue = parseInt(valueElement.textContent);
                    this.animateValue(valueElement, 0, finalValue, 1000);
                }
            }, index * 100);
        });
    }

    animateValue(element, start, end, duration) {
        const range = end - start;
        const startTime = performance.now();

        const updateValue = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);

            // Easing function (easeOutQuart)
            const easeProgress = 1 - Math.pow(1 - progress, 4);

            const currentValue = Math.floor(start + range * easeProgress);
            element.textContent = currentValue;

            if (progress < 1) {
                requestAnimationFrame(updateValue);
            } else {
                element.textContent = end; // Ensure final value is exact
            }
        };

        requestAnimationFrame(updateValue);
    }

    initCharts() {
        this.createMediaDistributionChart();
    }

    createMediaDistributionChart() {
        const ctx = document.getElementById('mediaDistributionChart');
        if (!ctx) return;

        const stats = window.statsData ?? {
            totalGames: 0,
            totalMovies: 0,
            totalBooks: 0,
            totalShows: 0,
        };

        const data = {
            games: stats.totalGames,
            movies: stats.totalMovies,
            books: stats.totalBooks,
            shows: stats.totalShows,
        };

        this.charts.mediaDistribution = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['🎮 Games', '🎬 Movies', '📚 Books', '📺 Shows'],
                datasets: [
                    {
                        data: [data.games, data.movies, data.books, data.shows],
                        backgroundColor: [
                            'rgba(59, 130, 246, 0.8)', // Blue for games
                            'rgba(239, 68, 68, 0.8)', // Red for movies
                            'rgba(34, 197, 94, 0.8)', // Green for books
                            'rgba(249, 115, 22, 0.8)', // Orange for shows
                        ],
                        borderColor: [
                            'rgba(59, 130, 246, 1)',
                            'rgba(239, 68, 68, 1)',
                            'rgba(34, 197, 94, 1)',
                            'rgba(249, 115, 22, 1)',
                        ],
                        borderWidth: 2,
                    },
                ],
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 15,
                            font: {
                                size: 13,
                                family: "'Inter', sans-serif",
                            },
                            usePointStyle: true,
                            pointStyle: 'circle',
                        },
                    },
                    tooltip: {
                        backgroundColor: 'rgba(0, 0, 0, 0.8)',
                        padding: 12,
                        titleFont: {
                            size: 14,
                            weight: 'bold',
                        },
                        bodyFont: {
                            size: 13,
                        },
                        callbacks: {
                            label: function (context) {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = ((value / total) * 100).toFixed(1);
                                return `${label}: ${value} (${percentage}%)`;
                            },
                        },
                    },
                },
                animation: {
                    animateRotate: true,
                    animateScale: true,
                    duration: 1500,
                    easing: 'easeInOutQuart',
                },
            },
        });
    }
}

export { Dashboard };

// Initialize dashboard when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new Dashboard();
});
