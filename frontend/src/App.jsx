import SearchForm from './components/SearchForm';
import ResultsList from './components/ResultsList';
import { useFlightSearch } from './hooks/useFlightSearch';

export default function App() {
  const { results, loading, error, lastQuery, search } = useFlightSearch();

  return (
    <div className="min-h-screen bg-slate-900 text-white">
      {/* Top bar — FIDS style */}
      <div className="bg-slate-950 border-b border-amber-400 border-opacity-30 px-6 py-2 flex items-center justify-between">
        <span className="font-mono text-amber-400 text-xs tracking-widest uppercase">SkyPath Systems v1.0</span>
        <span className="font-mono text-slate-500 text-xs tracking-widest">
          {new Date().toLocaleTimeString('en-US', { hour12: false })} UTC
        </span>
      </div>

      {/* Hero */}
      <div className="bg-slate-950 border-b border-slate-800 px-6 py-10">
        <div className="max-w-3xl mx-auto">
          <h1 className="text-4xl font-bold text-white mb-1" style={{ fontFamily: 'Sora, sans-serif' }}>
            Sky<span className="text-amber-400">Path</span>
          </h1>
          <p className="text-slate-400 text-sm font-mono tracking-wide mb-8">
            FLIGHT CONNECTION SEARCH ENGINE
          </p>
          <SearchForm onSearch={search} loading={loading} />
        </div>
      </div>

      {/* Results */}
      <div className="max-w-3xl mx-auto px-6 py-6 pb-16">
        <ResultsList
          results={results}
          loading={loading}
          error={error}
          lastQuery={lastQuery}
        />
      </div>
    </div>
  );
}
