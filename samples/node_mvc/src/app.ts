
import {Express} from "express";
import feathers from "@feathersjs/feathers";

import {main} from "gust/js/backend/app";
import {prepare} from "gust/js/backend/init";


/**
 * TypeScript FeathersJS controller, built/assembled/packaged by Gust.
 */
prepare((app: feathers.Application, express: Express) => {
    express.get('/', (request, response) => {
        response.send('Hello from TypeScript!');
    });
});

main();
