export function Limitations({ items }: { items: string[] }) {
  return (
    <section className="panel" aria-labelledby="limitaciones-titulo">
      <h2 id="limitaciones-titulo">Qué no puede afirmar este informe</h2>
      <ul>
        {items.map((item, index) => (
          <li key={index}>{item}</li>
        ))}
      </ul>
    </section>
  )
}
