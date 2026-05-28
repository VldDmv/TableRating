import { describe, test, expect } from '@jest/globals';
import { getAvatarColor } from '@/shared/avatarUtils.js';

describe('getAvatarColor', () => {
    describe('Determinism', () => {
        test('should return the same color for the same name', () => {
            expect(getAvatarColor('alice')).toBe(getAvatarColor('alice'));
        });

        test('should return the same color across multiple calls', () => {
            const color = getAvatarColor('bob');
            for (let i = 0; i < 10; i++) {
                expect(getAvatarColor('bob')).toBe(color);
            }
        });
    });

    describe('Return value', () => {
        test('should return a CSS gradient string', () => {
            const color = getAvatarColor('alice');
            expect(typeof color).toBe('string');
            expect(color).toContain('linear-gradient');
        });

        test('should return one of the 8 predefined gradients', () => {
            const color = getAvatarColor('alice');
            expect(color).toContain('#');
        });
    });

    describe('Edge cases', () => {
        test('should return a default color for empty string', () => {
            expect(getAvatarColor('')).toBeDefined();
            expect(getAvatarColor('')).toContain('linear-gradient');
        });

        test('should return a default color for null', () => {
            expect(getAvatarColor(null)).toBeDefined();
            expect(getAvatarColor(null)).toContain('linear-gradient');
        });

        test('should return a default color for undefined', () => {
            expect(getAvatarColor(undefined)).toContain('linear-gradient');
        });

        test('should handle long names', () => {
            expect(() => getAvatarColor('a'.repeat(1000))).not.toThrow();
        });

        test('should handle unicode names', () => {
            expect(() => getAvatarColor('Иван')).not.toThrow();
            expect(getAvatarColor('Иван')).toContain('linear-gradient');
        });
    });

    describe('Distribution', () => {
        test('different names should not all get the same color', () => {
            const colors = new Set(
                ['alice', 'bob', 'charlie', 'diana', 'eve', 'frank', 'grace', 'henry'].map(
                    getAvatarColor
                )
            );
            expect(colors.size).toBeGreaterThan(1);
        });
    });
});
