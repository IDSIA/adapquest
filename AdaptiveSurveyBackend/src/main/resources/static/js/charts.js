const colors = ['#e41a1c', '#377eb8', '#4daf4a', '#984ea3', '#ff7f00', '#ffff33', '#a65628', '#f781bf', '#999999']

const margin = {top: 10, right: 30, bottom: 30, left: 60};
const width = 500 - margin.left - margin.right;
const height = 400 - margin.top - margin.bottom;

// entropy chart
const svge = d3.select('#chart-entropy')
    .append('svg')
    .attr('width', width + margin.left + margin.right)
    .attr('height', height + margin.top + margin.bottom)
    .append('g')
    .attr('transform', `translate(${margin.left}, ${margin.top})`);

// distribution chart
const svgd = d3.select('#chart-distribution')
    .append('svg')
    .attr('width', width + margin.left + margin.right)
    .attr('height', height + margin.top + margin.bottom)
    .append('g')
    .attr('transform', `translate(${margin.left}, ${margin.top})`);

// following is the entropy lines chart
d3.json(
    '/survey/states/' + token,
    function (data) {
        let entropies = []

        data.forEach(d => {
            d.skills.forEach(s => {
                entropies.push({'answers': d.totalAnswers, 'skill': s.name, 'value': d.entropyDistribution[s.name]})
            });
        });

        // group the data: I want to draw one line per group
        let sumstat = d3.nest() // nest function allows to group the calculation per level of a factor
            .key((d) => d.skill)
            .entries(entropies);

        let x = d3.scaleLinear()
            .domain([0, d3.max(data, (d) => d.totalAnswers + 1)])
            .range([0, width]);
        svge.append('g')
            .attr('transform', `translate(0, ${height})`)
            .call(d3.axisBottom(x));

        let y = d3.scaleLinear()
            .domain([0, 1])
            .range([height, 0]);
        svge.append('g')
            .call(d3.axisLeft(y));

        let res = sumstat.map((d) => d.key);
        // list of group names
        let color = d3.scaleOrdinal()
            .domain(res)
            .range(colors.slice(0, res.length));

        svge.selectAll('.line')
            .data(sumstat)
            .enter()
            .append('path')
            .attr('fill', 'none')
            .attr('stroke', (d) => color(d.key))
            .attr('stroke-width', 1.5)
            .attr('d', (d) => {
                return d3.line()
                    .x((d) => x(d.answers))
                    .y((d) => y(+d.value))
                    (d.values)
            });
    }
);

// following is the entropy lines chart
d3.json(
    '/survey/state/' + token,
    function (data) {
        let distributions = []
        data.skills.forEach(s => {
            s.states.forEach(st => {
                distributions.push({
                    'name': `P(${s.name}=${st.name})`,
                    'value': data.skillDistribution[s.name][st.state]
                })
            });
        });

        let x = d3.scaleBand()
            .range([0, width])
            .domain(distributions.map((d) => d.name))
            .padding(0.2);

        svgd.append('g')
            .attr('transform', `translate(0, ${height})`)
            .call(d3.axisBottom(x));

        let y = d3.scaleLinear()
            .domain([0, 1])
            .range([height, 0]);
        svgd.append('g')
            .call(d3.axisLeft(y));

        // tooltip
        let tooltip = d3.select('#chart-distribution').append('div').attr('class', 'tooltip');

        let mouseOver = function (d) {
            let key = d.name;
            let value = d.value;
            tooltip.html(`${key} = ${value}`).style('opacity', 1);
        }
        let mouseMove = function (d) {
            const [x, y] = d3.mouse(this);
            tooltip
                .style("left", (x + 500) + "px")
                .style("top", (y + 100) + "px");
        }
        let mouseLeave = function (d) {
            tooltip.style("opacity", 0);
        }

        svgd.selectAll('mybar')
            .data(distributions)
            .enter()
            .append('rect')
            .attr('x', (d) => x(d.name))
            .attr('y', (d) => y(d.value))
            .attr('width', x.bandwidth())
            .attr('height', (d) => height - y(d.value))
            .attr('fill', '#69b3a2')
            .on("mouseover", mouseOver)
            .on("mousemove", mouseMove)
            .on("mouseleave", mouseLeave);
    }
);
