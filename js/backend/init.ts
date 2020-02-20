
import {Express} from 'express';
import express from '@feathersjs/express';
import feathers from '@feathersjs/feathers';

import {Logging} from 'gust/js/backend/logging';


/**
 * Main Feathers-powered application, which is used to initialize ExpressJS with middleware, controllers, services, and
 * so on. This app is eventually served from `app.ts`.
 */
const app: feathers.Application = feathers();

/** Express-style application, provided pre-wrapped around the main FeathersJS app. */
const expressApp: Express = express(app);


/**
 * Command type for a given buffered command.
 */
enum CommandType {
    /** Generic prep callback type. */
    GENERIC
}


/**
 * Describes the signature expected for an application init callback, which accepts the FeathersJS application being
 * initialized, and is not expected to return anything.
 */
type AppInitCallback = (app: feathers.Application, express: Express) => Promise<any> | undefined | void;

/**
 * Holds properties and a callback method that, together, specify a step that must be taken to initialize a backend app
 * running atop FeatherJS/Express.
 */
class QueuedAppInit {
    /** Type of command enqueued in this entry. */
    private readonly type: CommandType;

    /** Callback to execute the enqueued command. */
    private readonly callback: AppInitCallback;

    /**
     * Construct a new app init step, with the provided command type and callback, which should execute the requisite
     * code against the provided app parameters.
     *
     * @param {!CommandType} type Type of command being executed.
     * @param {!AppInitCallback} callback Callback function to dispatch when ready.
     */
    constructor(type: CommandType,
                callback: AppInitCallback) {
        this.type = type;
        this.callback = callback;
    }

    /**
     * Execute the attached callback, against the provided app objects.
     *
     * @param {!feathers.Application} app FeathersJS application.
     * @param {!Express} express ExpressJS-style application.
     */
    async execute(app: feathers.Application, express: Express) {
        try {
            const promise = this.callback(app, express);
            if (!!promise) {
                await promise;
                return;
            }
        } catch (err) {
            if (err instanceof Error) {
                throw new AppInitError(err.message, err);
            } else {
                throw new AppInitError("Unknown.", err);
            }
        }
    }
}

/**
 * Local command queue, used when buffering deferred calls that initialize or otherwise configure a backend application
 * running on NodeJS.
 */
const _queue: QueuedAppInit[] = [];


/**
 * Specialized exception class, thrown when a provided init callback fails or throws an error in some way, preventing
 * app startup and halting the process.
 */
export class AppInitError extends Error {
    private readonly context: any;

    constructor(message: string, context?: any) {
        super(`A fatal app init error occurred: "${message}'.`);
        this.context = context;
    }

    /**
     * Report the error to the console.
     */
    public report() {
        Logging.error(
            `An error occurred during app init: "${this.message}".`,
            this.context);
    }
}


/**
 * Enqueue a function to prepare an application, built atop FeathersJS or ExpressJS. The provided callback should accept
 * two parameters: `app` (the FeathersJS app), and `express` (the same app, but with an Express-compliant interface).
 *
 * @param {!AppInitCallback} config Function to dispatch.
 */
export function prepare(config: AppInitCallback) {
    _queue.push(new QueuedAppInit(CommandType.GENERIC, config));
}


/**
 * Boot the application, by executing any queued installation closures which mount stuff to `app`. Once these closures
 * are executed, the app can "boot," and it is returned to the calling function so it has a chance to do so.
 *
 * @return {!Express} Express-wrapped application.
 * @throws {AppInitError} If a deferred app init function fails or otherwise errors.
 */
export async function boot() {
    if (_queue.length < 1)
        throw new AppInitError("No tasks for bootstrap.");

    // perform init steps
    while (_queue.length) {
        let command = _queue.pop();
        if (!!command)
            await command.execute(app, expressApp);
        else
            break;
    }
    return expressApp;
}
