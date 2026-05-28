/**
 * Universal Autocomplete Module
 */

import { htmlUtils } from '../../core/utils.js';
import { ToastService } from '../../../shared/toast.js';

const notifiedDisabled = new Set();
const notifiedError = new Set();
let toastService = null;
function getToast() {
    if (!toastService) toastService = new ToastService();
    return toastService;
}

export class UniversalAutocomplete {
    constructor(inputElement, entityType, options = {}) {
        this.input = inputElement;
        this.entityType = entityType;
        this.options = {
            minChars: options.minChars || 2,
            debounceMs: options.debounceMs || 300,
            maxResults: options.maxResults || 10,
            ...options
        };

        this.coverUrlInput = null;
        this.lastSelectedCover = null;

        this.apiConfigs = {
            games: {
                apiUrl: '/api/proxy/games',
                buildUrl: (query) => {
                    const url = new URL(this.apiConfigs.games.apiUrl, window.location.origin);
                    url.searchParams.set('search', query);
                    url.searchParams.set('pageSize', this.options.maxResults);
                    return url;
                },
                parseResults: (data) => {
                    return (data.results || []).map(item => ({
                        name: item.name,
                        year: item.released ? new Date(item.released).getFullYear() : null,
                        rating: item.rating,
                        imageUrl: item.background_image,
                        coverUrl: item.background_image
                    }));
                }
            },
            movies: {
                apiUrl: '/api/proxy/movies',
                buildUrl: (query) => {
                    const url = new URL(this.apiConfigs.movies.apiUrl, window.location.origin);
                    url.searchParams.set('query', query);
                    return url;
                },
                parseResults: (data) => {
                    return (data.results || []).slice(0, this.options.maxResults).map(item => ({
                        name: item.title,
                        year: item.release_date ? new Date(item.release_date).getFullYear() : null,
                        rating: item.vote_average ? item.vote_average.toFixed(1) : null,
                        imageUrl: item.poster_path ? `https://image.tmdb.org/t/p/w92${item.poster_path}` : null,
                        coverUrl: item.poster_path ? `https://image.tmdb.org/t/p/w500${item.poster_path}` : null
                    }));
                }
            },
            shows: {
                apiUrl: '/api/proxy/shows',
                buildUrl: (query) => {
                    const url = new URL(this.apiConfigs.shows.apiUrl, window.location.origin);
                    url.searchParams.set('query', query);
                    return url;
                },
                parseResults: (data) => {
                    return (data.results || []).slice(0, this.options.maxResults).map(item => ({
                        name: item.name,
                        year: item.first_air_date ? new Date(item.first_air_date).getFullYear() : null,
                        rating: item.vote_average ? item.vote_average.toFixed(1) : null,
                        imageUrl: item.poster_path ? `https://image.tmdb.org/t/p/w92${item.poster_path}` : null,
                        coverUrl: item.poster_path ? `https://image.tmdb.org/t/p/w500${item.poster_path}` : null
                    }));
                }
            },
            books: {
                apiUrl: '/api/proxy/books',
                buildUrl: (query) => {
                    const url = new URL(this.apiConfigs.books.apiUrl, window.location.origin);
                    url.searchParams.set('q', query);
                    url.searchParams.set('maxResults', this.options.maxResults);
                    return url;
                },
                parseResults: (data) => {
                    return (data.docs || []).map(book => ({
                        name: book.title || 'Unknown',
                        year: book.first_publish_year || null,
                        rating: book.ratings_average ? book.ratings_average.toFixed(1) : null,
                        imageUrl: book.cover_i
                            ? `https://covers.openlibrary.org/b/id/${book.cover_i}-S.jpg`
                            : null,
                        coverUrl: book.cover_i
                            ? `https://covers.openlibrary.org/b/id/${book.cover_i}-M.jpg`
                            : null,
                        authors: book.author_name ? book.author_name.join(', ') : 'Unknown'
                    }));
                }
            }
        };

        this.debounceTimeout = null;
        this.dropdown = null;
        this.selectedIndex = -1;
        this.results = [];
        this.isOpen = false;

        this.init();
    }

    init() {
        if (!this.input) {
            console.error('UniversalAutocomplete: Input element not found');
            return;
        }

        if (!this.apiConfigs[this.entityType]) {
            console.error(`UniversalAutocomplete: Unsupported entity type: ${this.entityType}`);
            return;
        }

        this.createCoverUrlInput();
        this.createDropdown();
        this.attachEventListeners();
    }

    createCoverUrlInput() {
        this.coverUrlInput = document.createElement('input');
        this.coverUrlInput.type = 'hidden';
        this.coverUrlInput.name = 'coverUrl';
        this.coverUrlInput.value = '';

        this.input.parentNode.insertBefore(this.coverUrlInput, this.input.nextSibling);

    }

    createDropdown() {
        this.dropdown = document.createElement('div');
        this.dropdown.className = 'autocomplete-dropdown';
        this.dropdown.style.display = 'none';

        this.input.parentNode.style.position = 'relative';
        this.input.parentNode.appendChild(this.dropdown);
    }

    attachEventListeners() {
        this.input.addEventListener('input', (e) => this.handleInput(e));
        this.input.addEventListener('keydown', (e) => this.handleKeydown(e));
        this.input.addEventListener('blur', (e) => this.handleBlur(e));
        this.input.addEventListener('focus', (e) => this.handleFocus(e));

        this.dropdown.addEventListener('mousedown', (e) => {
            e.preventDefault();
        });

        this.dropdown.addEventListener('click', (e) => {
            const item = e.target.closest('.autocomplete-item');
            if (item) {
                this.selectItem(parseInt(item.dataset.index));
            }
        });

        document.addEventListener('click', (e) => {
            if (!this.input.contains(e.target) && !this.dropdown.contains(e.target)) {
                this.close();
            }
        });
    }

   handleInput(event) {
           const value = event.target.value.trim();
           if (this.coverUrlInput && this.coverUrlInput.value) {
               const currentResults = this.results.find(r => r.name === value);
               if (!currentResults) {
                   this.coverUrlInput.value = '';
                   this.lastSelectedCover = null;
               }
           }

        clearTimeout(this.debounceTimeout);

        if (value.length < this.options.minChars) {
            this.close();
            return;
        }

        this.debounceTimeout = setTimeout(() => {
            this.search(value);
        }, this.options.debounceMs);
    }

    handleKeydown(event) {
        if (!this.isOpen) return;

        switch (event.key) {
            case 'ArrowDown':
                event.preventDefault();
                this.moveSelection(1);
                break;
            case 'ArrowUp':
                event.preventDefault();
                this.moveSelection(-1);
                break;
            case 'Enter':
                event.preventDefault();
                if (this.selectedIndex >= 0) {
                    this.selectItem(this.selectedIndex);
                }
                break;
            case 'Escape':
                event.preventDefault();
                this.close();
                break;
        }
    }

    handleBlur(event) {
        setTimeout(() => {
            if (!this.dropdown.contains(document.activeElement)) {
                this.close();
            }
        }, 200);
    }

    handleFocus(event) {
        const value = event.target.value.trim();
        if (value.length >= this.options.minChars && this.results.length > 0) {
            this.open();
        }
    }

    async search(query) {
        try {
            this.showLoading();

            const config = this.apiConfigs[this.entityType];
            const url = config.buildUrl(query);



            const response = await fetch(url);

            if (response.status === 503) {
                if (!notifiedDisabled.has(this.entityType)) {
                    notifiedDisabled.add(this.entityType);
                    getToast().show(
                        `Autocomplete for ${this.entityType} is temporarily unavailable.`,
                        'warning'
                    );
                }
                this.close();
                return;
            }

            if (response.status === 429) {
                getToast().show('Too many requests. Please wait a moment.', 'warning');
                this.close();
                return;
            }

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            if (data.error) {
                if (!notifiedDisabled.has(this.entityType)) {
                    notifiedDisabled.add(this.entityType);
                    getToast().show(data.error, 'warning');
                }
                this.close();
                return;
            }

            this.results = config.parseResults(data);


            if (this.results.length > 0) {
                this.render();
                this.open();
            } else {
                this.showNoResults();
            }

        } catch (error) {
            console.error(`[Autocomplete:${this.entityType}]`, error);
            if (!notifiedError.has(this.entityType)) {
                notifiedError.add(this.entityType);
                getToast().show(
                    `Could not load ${this.entityType} suggestions. Check your connection.`,
                    'error'
                );
            }
            this.close();
        }
    }

    render() {
        this.dropdown.innerHTML = '';
        this.selectedIndex = -1;

        this.results.forEach((item, index) => {
            const element = this.createItem(item, index);
            this.dropdown.appendChild(element);
        });
    }

    createItem(item, index) {
        const element = document.createElement('div');
        element.className = 'autocomplete-item';
        element.dataset.index = index;

        if (item.imageUrl) {
            const img = document.createElement('img');
            img.src = item.imageUrl;
            img.className = 'autocomplete-item-image';
            img.alt = item.name;
            element.appendChild(img);
        }

        const textContainer = document.createElement('div');
        textContainer.className = 'autocomplete-item-text';

        const name = document.createElement('div');
        name.className = 'autocomplete-item-name';
        name.textContent = item.name;

        const info = document.createElement('div');
        info.className = 'autocomplete-item-info';

        const infoParts = [];

        if (this.entityType === 'books' && item.authors) {
            infoParts.push(item.authors);
        }

        if (item.year) {
            infoParts.push(item.year);
        }

        if (item.rating) {
            infoParts.push(`★ ${item.rating}`);
        }

        info.textContent = infoParts.join(' • ');

        textContainer.appendChild(name);
        if (infoParts.length > 0) {
            textContainer.appendChild(info);
        }

        element.appendChild(textContainer);

        return element;
    }

    moveSelection(direction) {
        const items = this.dropdown.querySelectorAll('.autocomplete-item');
        if (items.length === 0) return;

        if (this.selectedIndex >= 0) {
            items[this.selectedIndex].classList.remove('autocomplete-item-selected');
        }

        this.selectedIndex += direction;

        if (this.selectedIndex < 0) {
            this.selectedIndex = items.length - 1;
        } else if (this.selectedIndex >= items.length) {
            this.selectedIndex = 0;
        }

        items[this.selectedIndex].classList.add('autocomplete-item-selected');
        items[this.selectedIndex].scrollIntoView({ block: 'nearest' });
    }

    selectItem(index) {
        if (index < 0 || index >= this.results.length) return;

        const item = this.results[index];
        this.input.value = item.name;

        if (this.coverUrlInput && item.coverUrl) {
            this.coverUrlInput.value = item.coverUrl;
            this.lastSelectedCover = item.coverUrl;

        } else if (this.coverUrlInput) {

            this.coverUrlInput.value = '';
            this.lastSelectedCover = null;

        }

        this.input.dispatchEvent(new Event('input', { bubbles: true }));

        this.close();
        this.input.focus();
    }

    open() {
        this.dropdown.style.display = 'block';
        this.isOpen = true;
        this.input.setAttribute('aria-expanded', 'true');
    }

    close() {
        this.dropdown.style.display = 'none';
        this.isOpen = false;
        this.selectedIndex = -1;
        this.input.setAttribute('aria-expanded', 'false');
    }

    showLoading() {
        this.dropdown.innerHTML = '<div class="autocomplete-loading">Searching...</div>';
        this.open();
    }

    showNoResults() {
        const entityName = this.entityType.slice(0, -1);
        this.dropdown.innerHTML = `<div class="autocomplete-no-results">No ${entityName}s found</div>`;
        this.open();
    }

    destroy() {
        clearTimeout(this.debounceTimeout);
        if (this.dropdown && this.dropdown.parentNode) {
            this.dropdown.parentNode.removeChild(this.dropdown);
        }
        if (this.coverUrlInput && this.coverUrlInput.parentNode) {
            this.coverUrlInput.parentNode.removeChild(this.coverUrlInput);
        }
    }
}