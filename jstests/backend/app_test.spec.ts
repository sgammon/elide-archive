
import {DEFAULT_PORT} from "gust/js/backend/app";


describe(`'gust.js.backend': app`, () => {
    it('should expose a default port', () => {
        expect(DEFAULT_PORT).toBe(8080);
    });
});
