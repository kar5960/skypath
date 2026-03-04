import { useState } from 'react';

const AIRPORT_REGEX = /^[A-Z]{3}$/;
const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

function validate({ origin, destination, date }) {
  const errors = {};
  if (!origin) errors.origin = 'Origin is required';
  else if (!AIRPORT_REGEX.test(origin)) errors.origin = 'Must be 3 uppercase letters (e.g. JFK)';

  if (!destination) errors.destination = 'Destination is required';
  else if (!AIRPORT_REGEX.test(destination)) errors.destination = 'Must be 3 uppercase letters (e.g. LAX)';
  else if (destination === origin) errors.destination = 'Must differ from origin';

  if (!date) errors.date = 'Date is required';
  else if (!DATE_REGEX.test(date)) errors.date = 'Use YYYY-MM-DD format';

  return errors;
}

export default function SearchForm({ onSearch, loading }) {
  const [form, setForm] = useState({ origin: '', destination: '', date: '2024-03-15' });
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});

  function handleChange(e) {
    const { name, value } = e.target;
    const upper = name !== 'date' ? value.toUpperCase() : value;
    setForm(f => ({ ...f, [name]: upper }));
    if (touched[name]) {
      const errs = validate({ ...form, [name]: upper });
      setErrors(errs);
    }
  }

  function handleBlur(e) {
    const { name } = e.target;
    setTouched(t => ({ ...t, [name]: true }));
    setErrors(validate(form));
  }

  function handleSubmit(e) {
    e.preventDefault();
    const allTouched = { origin: true, destination: true, date: true };
    setTouched(allTouched);
    const errs = validate(form);
    setErrors(errs);
    if (Object.keys(errs).length === 0) {
      onSearch(form);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label className="block text-xs font-mono text-amber-400 uppercase tracking-widest mb-1">From</label>
          <input
            name="origin"
            value={form.origin}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="JFK"
            maxLength={3}
            className={`w-full bg-slate-800 border ${errors.origin && touched.origin ? 'border-red-500' : 'border-slate-600'} rounded px-4 py-3 font-mono text-lg text-white placeholder-slate-500 focus:outline-none focus:border-amber-400 transition-colors uppercase tracking-widest`}
          />
          {errors.origin && touched.origin && <p className="mt-1 text-xs text-red-400">{errors.origin}</p>}
        </div>

        <div>
          <label className="block text-xs font-mono text-amber-400 uppercase tracking-widest mb-1">To</label>
          <input
            name="destination"
            value={form.destination}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="LAX"
            maxLength={3}
            className={`w-full bg-slate-800 border ${errors.destination && touched.destination ? 'border-red-500' : 'border-slate-600'} rounded px-4 py-3 font-mono text-lg text-white placeholder-slate-500 focus:outline-none focus:border-amber-400 transition-colors uppercase tracking-widest`}
          />
          {errors.destination && touched.destination && <p className="mt-1 text-xs text-red-400">{errors.destination}</p>}
        </div>

        <div>
          <label className="block text-xs font-mono text-amber-400 uppercase tracking-widest mb-1">Date</label>
          <input
            name="date"
            type="date"
            value={form.date}
            onChange={handleChange}
            onBlur={handleBlur}
            className={`w-full bg-slate-800 border ${errors.date && touched.date ? 'border-red-500' : 'border-slate-600'} rounded px-4 py-3 text-white focus:outline-none focus:border-amber-400 transition-colors`}
          />
          {errors.date && touched.date && <p className="mt-1 text-xs text-red-400">{errors.date}</p>}
        </div>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="mt-6 w-full bg-amber-400 hover:bg-amber-300 disabled:bg-amber-900 disabled:cursor-not-allowed text-slate-900 font-bold py-3 px-8 rounded font-mono tracking-widest uppercase text-sm transition-colors"
      >
        {loading ? 'SEARCHING...' : 'SEARCH FLIGHTS'}
      </button>
    </form>
  );
}
