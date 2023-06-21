# Architecture Decision Records

ADRs are automatically published to our Log4brains architecture knowledge base:

🔗 **<http://INSERT-YOUR-LOG4BRAINS-URL>**

Please use this link to browse them.

## Development

If not already done, install Log4brains:

```bash
npm install -g log4brains
```

To preview the knowledge base locally, run:

```bash
log4brains preview
```

In preview mode, the Hot Reload feature is enabled: any change you make to a markdown file is applied live in the UI.

To create a new ADR interactively, run:

```bash
log4brains adr new
```

## Mermaid support

Log4brains does not support [Github Mermaid Diagrams](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/creating-diagrams) diagrams by default.

To successfully render Mermaid diagrams on the server side, add the following code to your ADR:
```
<pre class="mermaid">
  ... your mermaid code here ...
</pre>
<script src="https://cdnjs.cloudflare.com/ajax/libs/mermaid/9.2.1/mermaid.min.js"/>
<script>
  mermaid.initialize({ startOnLoad: true });
</script>
```

> Unfortunately, this diagram won't be automatically rendered in your preview mode.
> So, you could debug using github mermaid diagrams, but then integrate the code above in your ADR.

## More information

- [RFC-0016](https://input-output.atlassian.net/wiki/spaces/ATB/pages/3580559403/RFC+0016+-+Use+Architectural+Design+Records)
- [Engineering Guidance](https://input-output.atlassian.net/wiki/spaces/AV2/pages/3599237263/Architectural+Decision+Records+ADRs)
- [Log4brains documentation](https://github.com/thomvaill/log4brains/tree/master#readme)
- [What is an ADR and why should you use them](https://github.com/thomvaill/log4brains/tree/master#-what-is-an-adr-and-why-should-you-use-them)
- [ADR GitHub organization](https://adr.github.io/)