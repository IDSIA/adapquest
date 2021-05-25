const colors = ['#e41a1c', '#377eb8', '#4daf4a', '#984ea3', '#ff7f00', '#ffff33', '#a65628', '#f781bf', '#999999']

const margin = {top: 10, right: 30, bottom: 30, left: 60};
const width = 460 - margin.left - margin.right;
const height = 400 - margin.top - margin.bottom;

// entropy chart
let svg = d3.select('#chart-entropy')
    .append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
    .append("g")
    .attr("transform",
        "translate(" + margin.left + "," + margin.top + ")");

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
            .key(function (d) {
                return d.skill;
            })
            .entries(entropies);

        // Add X axis
        let x = d3.scaleLinear()
            .domain([0, d3.max(data, function (d) {
                return d.totalAnswers + 1;
            })])
            .range([0, width]);
        svg.append("g")
            .attr("transform", "translate(0," + height + ")")
            .call(d3.axisBottom(x));

        // Add Y axis
        let y = d3.scaleLinear()
            .domain([0, 1])
            .range([height, 0]);
        svg.append("g")
            .call(d3.axisLeft(y));

        let res = sumstat.map(function (d) {
            return d.key
        });
        // list of group names
        let color = d3.scaleOrdinal()
            .domain(res)
            .range(colors.slice(0, res.length));

        // Add the line
        svg.selectAll(".line")
            .data(sumstat)
            .enter()
            .append("path")
            .attr("fill", "none")
            .attr("stroke", function (d) {
                return color(d.key)
            })
            .attr("stroke-width", 1.5)
            .attr("d", function (d) {
                return d3.line()
                    .x(function (d) {
                        return x(d.answers);
                    })
                    .y(function (d) {
                        return y(+d.value);
                    })
                    (d.values)
            })
    }
);

