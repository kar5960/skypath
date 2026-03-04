import ItineraryCard from './ItineraryCard';

function LoadingState() {
  return (
    <div className="flex flex-col items-center justify-center py-20 gap-4">
      <div className="w-12 h-12 border-2 border-amber-400 border-t-transparent rounded-full animate-spin"></div>
      <p className="text-slate-400 font-mono text-sm tracking-widest uppercase">Scanning routes...</p>
    </div>
  );
}

function ErrorState({ message }) {
  return (
    <div className="bg-red-950 border border-red-800 rounded-lg px-6 py-5 mt-6">
      <div className="flex items-start gap-3">
        <span className="text-red-400 text-lg">⚠</span>
        <div>
          <p className="text-red-300 font-mono text-sm font-bold uppercase tracking-wide">Search Error</p>
          <p className="text-red-200 mt-1 text-sm">{message}</p>
        </div>
      </div>
    </div>
  );
}

function EmptyState({ query }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 gap-3">
      <div className="text-5xl">✈</div>
      <p className="text-white font-mono font-bold">No routes found</p>
      <p className="text-slate-400 text-sm text-center max-w-sm">
        No valid itineraries found from <span className="text-amber-400 font-mono">{query?.origin}</span> to{' '}
        <span className="text-amber-400 font-mono">{query?.destination}</span> on {query?.date}.
        Try a different date.
      </p>
    </div>
  );
}

export default function ResultsList({ results, loading, error, lastQuery }) {
  if (loading) return <LoadingState />;
  if (error) return <ErrorState message={error} />;
  if (!results) return null;

  const { itineraries, totalResults } = results;

  if (totalResults === 0) return <EmptyState query={lastQuery} />;

  return (
    <div className="mt-8">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-white font-mono font-bold tracking-wide">
          {totalResults} {totalResults === 1 ? 'itinerary' : 'itineraries'} found
        </h2>
        <span className="text-slate-400 text-xs font-mono">sorted by travel time</span>
      </div>

      <div className="flex flex-col gap-4">
        {itineraries.map((itinerary, i) => (
          <ItineraryCard key={i} itinerary={itinerary} index={i} />
        ))}
      </div>
    </div>
  );
}
