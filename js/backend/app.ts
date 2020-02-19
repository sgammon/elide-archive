
import feathers from '@feathersjs/feathers';

import {Logging} from 'gust/js/backend/logging';
import {boot, AppInitError} from 'gust/js/backend/init';


/**
 * Main application entrypoint. This is dispatched by Gust upon running the application, and is responsible for finding/
 * loading user controllers, middleware, services, etc., and initializing them in the backend environment.
 *
 * After deferred initialization completes, the server begins listening on the port provided via the optional parameter,
 * then falls back to the environment variable `PORT`, then falls back to `8080`.
 *
 * @param {number=} port Optional port to listen on.
 * @returns {function(): void}
 */
export const main = async (port?: number) => {
  Logging.log('Loading FeathersJS application...');

  try {
    const app: feathers.Application = await boot();

    // load or select port
    const listenPort = port || (process.env.PORT ? parseInt(process.env.PORT) : 8080);
    if (listenPort > 65535 || listenPort < -1) {
      Logging.error(`FAILURE: Invalid port selection "${listenPort}".`);
      process.exit(1);
    }

    // listen for traffic and begin serving
    Logging.log(`Listening on port ${listenPort}...`);
    const server = app.listen(listenPort);

    process.on('unhandledRejection', (reason, p) =>
        console.error('Unhandled Rejection at: Promise ', p, reason));

    server.on('listening', () =>
        Logging.info(
            'Feathers application started on http://%s:%d',
            app.get('host'),
            port));

  } catch (err) {
    if (err instanceof AppInitError) {
      err.report();

    } else {
      Logging.error(`Unrecognized fatal error occurred: '${err}'.`);
      process.exit(1);
    }
  }
};
