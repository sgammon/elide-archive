/*
 * Copyright © 2020, The Gust Framework Authors. All rights reserved.
 *
 * The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
 * are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
 * this code in object or source form requires and implies consent and agreement to that license in principle and
 * practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
 * Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
 * Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
 * by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
 * is strictly forbidden except in adherence with assigned license requirements.
 */
import {Express} from 'express';
import feathers from '@feathersjs/feathers';

import {Logging} from 'gust/js/backend/logging';
import {boot, AppInitError} from 'gust/js/backend/init';


/**
 * Default port, used if no port is defined at the environment variable `PORT`, or otherwise explicitly passed to the
 * app `main` bootstrap function.
 */
export const DEFAULT_PORT = 8080;


/**
 * Main application entrypoint. This is dispatched by Gust upon running the application, and is responsible for finding/
 * loading user controllers, middleware, services, etc., and initializing them in the backend environment.
 *
 * After deferred initialization completes, the server begins listening on the port provided via the optional parameter,
 * then falls back to the environment variable `PORT`, then falls back to `8080`.
 *
 * @param {number=} port Optional port to listen on.
 * @param {!boolean} listen Whether to listen on behalf of the invoking code.
 * @returns {function(): void}
 */
export const main = async (port?: number, listen: boolean = true) => {
  Logging.log('Loading FeathersJS application...');

  try {
    const app: Express = await boot();
    const hostname = app.get('host') || 'localhost';

    // load or select port
    const listenPort = port || (process.env.PORT ? parseInt(process.env.PORT) : DEFAULT_PORT);
    if (listenPort > 65535 || listenPort < -1) {
      Logging.error(`FAILURE: Invalid port selection "${listenPort}".`);
      process.exit(1);
    }

    if (listen === true) {
      // listen for traffic and begin serving
      Logging.log(`Listening on port ${listenPort}...`);
      const server = app.listen(listenPort);

      process.on('unhandledRejection', (reason, p) =>
          console.error('Unhandled Rejection at: Promise ', p, reason));

      server.on('listening', () =>
          console.info(
              'Feathers application started on http://%s:%d',
              hostname,
              listenPort));
    }

  } catch (err) {
    if (err instanceof AppInitError) {
      err.report();

    } else {
      Logging.error(`Unrecognized fatal error occurred: '${err}'.`);
      process.exit(1);
    }
  }
};
