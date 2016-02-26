import LinearProgress from 'material-ui/lib/linear-progress';

export default function Browser({running, max, name}) {
    return (
        <div>
            <div style={{padding: '0 30px 10px 0'}}>
                {name}
                <b style={{float: 'right'}}>{running}/{max}</b>
            </div>
            <LinearProgress mode="determinate"
                            min={0}
                            max={max}
                            value={running}
            />
        </div>
    );
}
