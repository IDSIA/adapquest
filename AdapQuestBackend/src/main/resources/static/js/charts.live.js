// code adapted from https://www.d3-graph-gallery.com/ and https://observablehq.com/@bjedwards/multi-line-chart-d3-v6

const margin = {top: 20, right: 30, bottom: 60, left: 60};
const width = 1000 - margin.left - margin.right;
const height = 400 - margin.top - margin.bottom;

// following is the distribution bar chart
d3.json(context + 'survey/state/' + token)
    .then(data => {
        const distributions = []
        data.skills.forEach(s => {
            // s.states.forEach(st => {
            distributions.push({
                'name': `${s.name}`,
                'value': data.skillDistribution[s.name][1].toFixed(2)
                // })
            });
        });
        return distributions;
    })
    .then((data) => {
        const svg = d3.select('#chart-distribution')
            .append('svg')
            .attr('width', width + margin.left + margin.right)
            .attr('height', 1.7*height + margin.top + margin.bottom)
            .append('g')
            .attr('transform', `translate(${margin.left}, ${margin.top})`);

        const tooltip = d3.select('#chart-distribution').append('div').attr('class', 'bar-tooltip');

        const x = d3.scaleBand()
            .range([0, width])
            .domain(data.map(d => d.name))
            .padding(0.2);
        svg.append('g')
            .attr('transform', `translate(0, ${height})`)
            .call(d3.axisBottom(x))
            .selectAll('text')
            .attr('transform', 'translate(-12,+10)rotate(-90)')
            .style('text-anchor', 'end');

        const y = d3.scaleLinear()
            .domain([0, 1])
            .range([height, 0]);
        svg.append('g')
            .call(d3.axisLeft(y));

        const mouseOver = (event, d) => {
            const key = d.name;
            const value = d.value;
            tooltip.html(`${key} = ${value}`).style('opacity', 1);
        }
        const mouseMove = (event, d) => {
            tooltip
                .style('left', (event.x) + 'px')
                .style('top', (event.y + 20) + 'px');
        }
        let mouseLeave = (event, d) => {
            tooltip.style('opacity', 0);
        }

        svg.selectAll('mybar')
            .data(data)
            .enter()
            .append('rect')
            .attr('x', d => x(d.name))
            .attr('y', d => y(d.value))
            .attr('width', x.bandwidth())
            .attr('height', d => height - y(d.value))
            .attr('fill', '#e77327')
            .on('mouseover', mouseOver)
            .on('mousemove', mouseMove)
            .on('mouseleave', mouseLeave);
    });
