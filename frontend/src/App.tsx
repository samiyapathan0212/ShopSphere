import { useGetHealthQuery } from './app/api/apiSlice';

function App() {
  const { data, isLoading, isError } = useGetHealthQuery();

  return (
    <main className="app">
      <h1>ShopSphere</h1>
      <p>Phase 1 — project foundation</p>

      <section className="health" aria-labelledby="health-title">
        <h2 id="health-title">Backend health</h2>
        {isLoading && <p>Checking backend…</p>}
        {isError && (
          <p className="error">
            Backend is unreachable. Is the API running locally or in Docker?
          </p>
        )}
        {data && (
          <dl>
            <div>
              <dt>Status</dt>
              <dd>{data.status}</dd>
            </div>
            <div>
              <dt>Service</dt>
              <dd>{data.service}</dd>
            </div>
          </dl>
        )}
      </section>
    </main>
  );
}

export default App;