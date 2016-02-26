import { Component } from 'react';
import DropDownMenu from 'material-ui/lib/DropDownMenu';
import MenuItem from 'material-ui/lib/menus/menu-item';

export default class QuotaSelector extends Component {

    constructor(props) {
        super(props);
        this.state = {selected: 0};
        this.handleChange = this.handleChange.bind(this);
    }

    handleChange(event, index, value) {
        this.setState({selected: value});
        if (this.props.onChange) {
            this.props.onChange(event.target.textContent);
        }
    }

    renderItems() {
        return this.props.quotas.map((quota, index) => {
            return (
                <MenuItem
                    key={index}
                    value={index}
                    primaryText={quota.name}/>
            );
        });
    }

    render() {
        return (
            <DropDownMenu
                value={this.state.selected}
                onChange={this.handleChange}
                disabled={false}
                labelStyle={{color: '#fff'}}>
                {this.renderItems()}
            </DropDownMenu>
        );
    }
}