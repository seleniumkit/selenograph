import { Component } from 'react';
import keys from 'lodash/keys';
import Avatar from 'material-ui/lib/avatar';
import List from 'material-ui/lib/lists/list';
import ListItem from 'material-ui/lib/lists/list-item';
import AppBar from 'material-ui/lib/app-bar';
import QuotaSelector from './QuotaSelector.jsx';
import Browser from './Browser.jsx';
import BrowserVersion from './BrowserVersion.jsx';
import fetch from '../util/fetch.js';
import WS from './../util/websocket.js';

const ENDPOINTS = {
    hubs: '/api/selenograph/hubs',
    quotas: '/api/selenograph/quotas'
};

const BROWSER_ICONS = {
    opera: require('../icons/browsers/opera/opera_128x128.png'),
    chrome: require('../icons/browsers/chrome/chrome_128x128.png'),
    android: require('../icons/browsers/android/android_128x128.png'),
    firefox: require('../icons/browsers/firefox/firefox_128x128.png'),
    safari: require('../icons/browsers/safari/safari_128x128.png'),
    ios: require('../icons/browsers/safari-ios/safari-ios_128x128.png'),
    microsoftedge: require('../icons/browsers/edge/edge_128x128.png'),
    'internet explorer': require('../icons/browsers/internet-explorer/internet-explorer_128x128.png')
};

export default class Browsers extends Component {
    state = {browsersList: [], quotas: [], quotaMap: {}};

    constructor(props) {
        super(props);
        this.quotaChanged = this.quotaChanged.bind(this);
    }

    componentDidMount() {
        fetch(ENDPOINTS.quotas)
            .then(quotas => {
                this.websocket = new WS('?pluginId=quotaClientNotifier', quotas => {
                    this.updateQuotas(quotas);
                });
                this.updateQuotas(quotas);
            });
    }

    componentWillUnmount() {
        if(this.websocket) {
            this.websocket.destroy();
        }
    }

    updateQuotas(quotas) {
        this.setState({
            browsersList: quotas.all,
            quotas: keys(quotas).map(s => {
                return {name: s};
            }),
            quotaMap: quotas
        });
    }

    quotaChanged(quota) {
        this.setState({
            browsersList: this.state.quotaMap[quota]
        });
    }

    renderVersions(versions) {
        return versions.map(ver =>
            <ListItem key={ver.version} primaryText={
                <BrowserVersion version={ver.version} max={ver.max} running={ver.running}/>
            }/>
        );
    }

    renderBrowsers(browsers) {
        return browsers.map(item =>
            <ListItem key={item.name}
                      leftAvatar={<Avatar src={BROWSER_ICONS[item.name.toLowerCase()]} />}
                      primaryText={ <Browser {...item} /> }
                      primaryTogglesNestedList={true}
                      nestedItems={ this.renderVersions(item.versions) }/>
        );
    }

    render() {
        return (
            <div>
                <AppBar title="Selenograph">
                    <QuotaSelector quotas={this.state.quotas} onChange={this.quotaChanged}/>
                </AppBar>
                <List>{this.renderBrowsers(this.state.browsersList)}</List>
            </div>
        );
    }
}

