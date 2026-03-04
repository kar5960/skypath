function formatTime(isoString) {
  if (!isoString) return '--';
  const parts = isoString.split('T');
  if (parts.length < 2) return isoString;
  return parts[1].substring(0, 5); // HH:MM
}

function StopBadge({ stops }) {
  if (stops === 0) return <span className="px-2 py-0.5 bg-emerald-900 text-emerald-300 text-xs font-mono rounded">DIRECT</span>;
  if (stops === 1) return <span className="px-2 py-0.5 bg-slate-700 text-slate-300 text-xs font-mono rounded">1 STOP</span>;
  return <span className="px-2 py-0.5 bg-slate-700 text-slate-300 text-xs font-mono rounded">2 STOPS</span>;
}

function SegmentRow({ segment }) {
  return (
    <div className="flex items-center gap-3 py-2">
      <div className="text-center w-16">
        <div className="font-mono text-lg font-bold text-white">{formatTime(segment.departureTime)}</div>
        <div className="text-xs text-amber-400 font-mono">{segment.originCode}</div>
        <div className="text-xs text-slate-400 truncate max-w-16">{segment.originCity}</div>
      </div>

      <div className="flex-1 flex flex-col items-center gap-1">
        <div className="text-xs text-slate-400 font-mono">{segment.flightNumber} · {segment.durationFormatted}</div>
        <div className="w-full flex items-center gap-1">
          <div className="w-2 h-2 rounded-full border border-amber-400"></div>
          <div className="flex-1 border-t border-dashed border-slate-600"></div>
          <svg className="w-4 h-4 text-amber-400" fill="currentColor" viewBox="0 0 24 24">
            <path d="M21 16v-2l-8-5V3.5c0-.83-.67-1.5-1.5-1.5S10 2.67 10 3.5V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z"/>
          </svg>
        </div>
        <div className="text-xs text-slate-500 font-mono">{segment.aircraft}</div>
      </div>

      <div className="text-center w-16">
        <div className="font-mono text-lg font-bold text-white">{formatTime(segment.arrivalTime)}</div>
        <div className="text-xs text-amber-400 font-mono">{segment.destinationCode}</div>
        <div className="text-xs text-slate-400 truncate max-w-16">{segment.destinationCity}</div>
      </div>
    </div>
  );
}

function LayoverBar({ layover }) {
  return (
    <div className="mx-4 my-1 px-3 py-1.5 bg-slate-700 border-l-2 border-amber-400 rounded-r text-xs text-slate-300 font-mono">
      ⏱ {layover.durationFormatted} layover · {layover.airportCode} ({layover.airportCity})
    </div>
  );
}

export default function ItineraryCard({ itinerary, index }) {
  return (
    <div className="bg-slate-800 border border-slate-700 hover:border-amber-400 transition-colors rounded-lg overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-5 py-3 border-b border-slate-700 bg-slate-900">
        <div className="flex items-center gap-3">
          <StopBadge stops={itinerary.stops} />
          <span className="text-slate-400 text-xs font-mono">#{index + 1}</span>
        </div>
        <div className="flex items-center gap-6">
          <div className="text-right">
            <div className="text-xs text-slate-400 font-mono uppercase tracking-wide">Duration</div>
            <div className="text-white font-mono font-bold">{itinerary.totalDurationFormatted}</div>
          </div>
          <div className="text-right">
            <div className="text-xs text-slate-400 font-mono uppercase tracking-wide">Total</div>
            <div className="text-amber-400 font-mono font-bold text-lg">${itinerary.totalPrice.toFixed(2)}</div>
          </div>
        </div>
      </div>

      {/* Segments + layovers */}
      <div className="px-5 py-2">
        {itinerary.segments.map((seg, i) => (
          <div key={seg.flightNumber + i}>
            <SegmentRow segment={seg} />
            {itinerary.layovers[i] && <LayoverBar layover={itinerary.layovers[i]} />}
          </div>
        ))}
      </div>
    </div>
  );
}
