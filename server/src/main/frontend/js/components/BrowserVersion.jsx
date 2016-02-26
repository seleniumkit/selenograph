import LinearProgress from 'material-ui/lib/linear-progress';

export default function BrowserVersion({running, max, version}) {
    return (
        <div>
            <div style={{paddingBottom: '10px'}}>
                {version}
                <b style={{float: 'right'}}>{running}/{max}</b>
            </div>
            <LinearProgress mode="determinate"
                            value={running}
                            max={max}
            />
        </div>
    );
}
