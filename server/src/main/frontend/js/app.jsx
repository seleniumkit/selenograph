import Browsers from './components/Browsers.jsx';
import { createElement } from 'react';
import { render } from 'react-dom';
import injectTapEventPlugin from 'react-tap-event-plugin';

injectTapEventPlugin();
render(
    createElement(Browsers),
    document.getElementById('app')
);