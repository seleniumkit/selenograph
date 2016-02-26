declare module 'webpack-dev-server' {
    import * as webpack from 'webpack';
    import {RequestHandler} from 'express';

    class WebpackDevServer {
        constructor(compiler:webpack.compiler.Compiler, configuration:Object)

        use(...middlewares:RequestHandler[])

        listen(port:Number, callback?:Function)
    }

    export = WebpackDevServer;
}
