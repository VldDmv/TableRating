import { ToastService } from '../../shared/toast.js';
import { securityUtils } from '../core/utils.js';

/**
 * Wires the per-category import file input. Reads the selected file,
 * POSTs it to /api/import/{category}, then reloads the page so the table
 * picks up the new rows.
 */
export function initImportExport() {
    const toast = new ToastService();
    const inputs = document.querySelectorAll('.import-file-input');

    inputs.forEach((input) => {
        input.addEventListener('change', async (e) => {
            const file = e.target.files && e.target.files[0];
            const category = input.dataset.category;
            if (!file || !category) return;

            const formData = new FormData();
            formData.append('file', file);

            const csrfToken = securityUtils.getCsrfToken();
            const headers = {};
            if (csrfToken) headers[securityUtils.getCsrfHeader()] = csrfToken;

            try {
                const response = await fetch(`/api/import/${category}`, {
                    method: 'POST',
                    body: formData,
                    headers,
                });

                if (!response.ok) {
                    toast.show(`Import failed (HTTP ${response.status})`, 'error');
                    return;
                }

                const result = await response.json();
                const summary = `Imported ${result.imported}, skipped ${result.skipped}`;
                if (result.imported > 0) {
                    toast.show(summary, 'success');
                    setTimeout(() => window.location.reload(), 800);
                } else {
                    toast.show(summary, 'warning');
                }
                if (Array.isArray(result.errors) && result.errors.length > 0) {
                    console.warn('[Import errors]', result.errors);
                }
            } catch (err) {
                console.error('[Import]', err);
                toast.show('Import failed: could not reach the server', 'error');
            } finally {
                input.value = '';
            }
        });
    });
}
