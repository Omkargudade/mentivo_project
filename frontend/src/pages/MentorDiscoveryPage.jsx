import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { MentorCard } from '../components/mentor/MentorCard';
import { BookingModal } from '../components/booking/BookingModal';
import { Button } from '../components/common/Button';
import { useSocket } from '../context/SocketContext';
import api from '../../src/api/client';
import { Search, SlidersHorizontal, RotateCcw, Sparkles, ChevronLeft, ChevronRight, X, Filter } from 'lucide-react';
export const MentorDiscoveryPage = () => {
    const { subscribeToMentorStatus } = useSocket();
    const [searchParams, setSearchParams] = useSearchParams();
    const [mentors, setMentors] = useState([]);
    const [categories, setCategories] = useState([]);
    const [topics, setTopics] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    // Filters State
    const [query, setQuery] = useState(searchParams.get('query') || '');
    const [debouncedQuery, setDebouncedQuery] = useState(query);
    const [selectedCategory, setSelectedCategory] = useState(searchParams.get('categoryId') || '');
    const [selectedTopic, setSelectedTopic] = useState(searchParams.get('topicId') || '');
    const [selectedLanguage, setSelectedLanguage] = useState(searchParams.get('language') || '');
    const [availableNowOnly, setAvailableNowOnly] = useState(searchParams.get('onlineStatus') === 'ONLINE');
    const [freeOnly, setFreeOnly] = useState(searchParams.get('freeOnly') === 'true');
    const [maxPrice, setMaxPrice] = useState(Number(searchParams.get('maxPrice')) || 1200);
    const [minRating, setMinRating] = useState(Number(searchParams.get('minRating')) || 0);
    const [minExperience, setMinExperience] = useState(Number(searchParams.get('minExperience')) || 0);
    const [sortBy, setSortBy] = useState(searchParams.get('sortBy') || 'relevance');
    const [page, setPage] = useState(Number(searchParams.get('page')) || 0);
    const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
    const [selectedBookingMentor, setSelectedBookingMentor] = useState(null);
    // Debounce search query
    useEffect(() => {
        const handler = setTimeout(() => {
            setDebouncedQuery(query);
            setPage(0);
        }, 350);
        return () => clearTimeout(handler);
    }, [query]);

    // Sync state with URL searchParams (e.g. when navigating from CategoriesPage or Navbar)
    useEffect(() => {
        const urlCat = searchParams.get('categoryId') || '';
        const urlTopic = searchParams.get('topicId') || '';
        const urlQuery = searchParams.get('query') || '';
        if (urlCat !== selectedCategory) setSelectedCategory(urlCat);
        if (urlTopic !== selectedTopic) setSelectedTopic(urlTopic);
        if (urlQuery !== query) {
            setQuery(urlQuery);
            setDebouncedQuery(urlQuery);
        }
    }, [searchParams]);
    // Load categories and topics
    useEffect(() => {
        api.get('/categories')
            .then((res) => {
                if (res.data?.success && res.data.data) {
                    setCategories(res.data.data);
                }
            })
            .catch(() => { });
        api.get('/topics')
            .then((res) => {
                if (res.data?.success && res.data.data) {
                    setTopics(res.data.data);
                }
            })
            .catch(() => { });
    }, []);

    // Live mentor status sync
    useEffect(() => {
        const unsubscribe = subscribeToMentorStatus((card) => {
            if (card?.id) {
                setMentors((prev) => prev.map((m) => m.id === card.id ? { ...m, onlineStatus: card.onlineStatus, lastSeen: card.lastSeen } : m));
            }
        });
        return () => {
            if (unsubscribe) unsubscribe();
        };
    }, [subscribeToMentorStatus]);
    // Fetch mentors when filters or page changes
    useEffect(() => {
        fetchMentors();
    }, [
        debouncedQuery,
        selectedCategory,
        selectedTopic,
        selectedLanguage,
        availableNowOnly,
        freeOnly,
        maxPrice,
        minRating,
        minExperience,
        sortBy,
        page,
    ]);
    const fetchMentors = async () => {
        setIsLoading(true);
        try {
            const params = new URLSearchParams();
            if (debouncedQuery.trim())
                params.set('query', debouncedQuery.trim());
            if (selectedCategory)
                params.set('categoryId', selectedCategory);
            if (selectedTopic)
                params.set('topicId', selectedTopic);
            if (selectedLanguage)
                params.set('language', selectedLanguage);
            if (availableNowOnly)
                params.set('onlineStatus', 'ONLINE');
            if (freeOnly) {
                params.set('freeOnly', 'true');
            }
            else if (maxPrice < 1200) {
                params.set('maxPrice', maxPrice.toString());
            }
            if (minRating > 0)
                params.set('minRating', minRating.toString());
            if (minExperience > 0)
                params.set('minExperience', minExperience.toString());
            if (sortBy)
                params.set('sortBy', sortBy);
            params.set('page', page.toString());
            params.set('size', '12');
            const res = await api.get(`/mentors?${params.toString()}`);
            if (res.data?.success && res.data.data) {
                setMentors(res.data.data.content || []);
                setTotalElements(res.data.data.totalElements || 0);
                setTotalPages(res.data.data.totalPages || 0);
            }
        }
        catch {
            // Fallback
        }
        finally {
            setIsLoading(false);
        }
    };
    const handleResetFilters = () => {
        setQuery('');
        setDebouncedQuery('');
        setSelectedCategory('');
        setSelectedTopic('');
        setSelectedLanguage('');
        setAvailableNowOnly(false);
        setFreeOnly(false);
        setMaxPrice(1200);
        setMinRating(0);
        setMinExperience(0);
        setSortBy('relevance');
        setPage(0);
    };
    const languagesList = ['English', 'Hindi', 'Kannada', 'Tamil', 'Marathi', 'Bengali', 'Telugu'];
    return (<div className="min-h-screen bg-slate-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Page Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2">
              <span>Find a Mentor</span>
              <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                {totalElements} available
              </span>
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              Connect with experienced people who can guide you 1-on-1.
            </p>
          </div>

          {/* Search Input & Mobile Filter Toggle */}
          <div className="flex items-center gap-2 w-full md:w-auto">
            <div className="relative flex-1 md:w-80">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2"/>
              <input type="text" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search skill, topic, or mentor..." className="w-full text-xs font-medium pl-9 pr-8 py-2.5 bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-2xs"/>
              {query && (<button onClick={() => setQuery('')} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">
                  <X className="w-3.5 h-3.5"/>
                </button>)}
            </div>

            <Button variant="outline" size="md" onClick={() => setMobileFiltersOpen(true)} className="md:hidden gap-1.5 px-3">
              <Filter className="w-4 h-4"/>
              <span>Filters</span>
            </Button>
          </div>
        </div>

        {/* Main 2-Column Layout */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 items-start">
          {/* Desktop Filter Sidebar */}
          <aside className="hidden md:block bg-white p-5 rounded-2xl border border-slate-200/80 shadow-2xs space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-900 flex items-center gap-1.5">
                <SlidersHorizontal className="w-3.5 h-3.5 text-indigo-600"/>
                Filters
              </span>
              <button onClick={handleResetFilters} className="text-[11px] text-slate-400 hover:text-indigo-600 flex items-center gap-1 font-medium cursor-pointer">
                <RotateCcw className="w-3 h-3"/>
                Reset
              </button>
            </div>

            {/* Availability Toggle */}
            <div>
              <label className="flex items-center justify-between cursor-pointer group">
                <span className="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"/>
                  Available Now Only
                </span>
                <input type="checkbox" checked={availableNowOnly} onChange={(e) => {
            setAvailableNowOnly(e.target.checked);
            setPage(0);
        }} className="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500 cursor-pointer"/>
              </label>
            </div>

            {/* Free Mentoring Toggle */}
            <div className="p-3 bg-emerald-50/70 border border-emerald-200/80 rounded-xl">
              <label className="flex items-center justify-between cursor-pointer group">
                <span className="text-xs font-bold text-emerald-800 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-emerald-600"/>
                  Free Sessions Only (₹0)
                </span>
                <input type="checkbox" checked={freeOnly} onChange={(e) => {
            setFreeOnly(e.target.checked);
            setPage(0);
        }} className="w-4 h-4 text-emerald-600 rounded border-emerald-300 focus:ring-emerald-500 cursor-pointer"/>
              </label>
              <p className="text-[10px] text-emerald-700 mt-1">
                Filter verified mentors offering ₹0/hr free community learning.
              </p>
            </div>

            {/* Category Filter */}
            <div>
              <label className="block text-xs font-semibold text-slate-800 mb-2">Category</label>
              <select value={selectedCategory} onChange={(e) => {
            setSelectedCategory(e.target.value);
            setSelectedTopic('');
            setPage(0);
        }} className="w-full text-xs border border-slate-200 rounded-xl p-2 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500">
                <option value="">All Categories</option>
                {categories.map((c) => (<option key={c.id} value={c.id}>
                    {c.name}
                  </option>))}
              </select>
            </div>

            {/* Topic Filter */}
            <div>
              <label className="block text-xs font-semibold text-slate-800 mb-2">Topic</label>
              <select value={selectedTopic} onChange={(e) => {
            setSelectedTopic(e.target.value);
            setPage(0);
        }} className="w-full text-xs border border-slate-200 rounded-xl p-2 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500">
                <option value="">All Topics</option>
                {topics
            .filter((t) => !selectedCategory || t.categoryId === Number(selectedCategory))
            .map((t) => (<option key={t.id} value={t.id}>
                      {t.name}
                    </option>))}
              </select>
            </div>

            {/* Max Hourly Rate Slider */}
            <div>
              <div className="flex justify-between text-xs font-semibold text-slate-800 mb-1.5">
                <span>Max Hourly Price</span>
                <span className="text-indigo-600 font-bold">
                  {maxPrice === 0 ? '₹0 (Free Only)' : `₹${maxPrice}/hr`}
                </span>
              </div>
              <input type="range" min={0} max={1200} step={50} value={maxPrice} disabled={freeOnly} onChange={(e) => {
            setMaxPrice(Number(e.target.value));
            setPage(0);
        }} className={`w-full accent-indigo-600 cursor-pointer ${freeOnly ? 'opacity-40 cursor-not-allowed' : ''}`}/>
              <div className="flex justify-between text-[10px] text-slate-400 mt-1">
                <span>₹0 (Free)</span>
                <span>₹1200+</span>
              </div>
            </div>

            {/* Minimum Rating */}
            <div>
              <label className="block text-xs font-semibold text-slate-800 mb-2">Minimum Rating</label>
              <div className="grid grid-cols-4 gap-1">
                {[0, 4.0, 4.5, 4.8].map((stars) => (<button key={stars} type="button" onClick={() => {
                setMinRating(stars);
                setPage(0);
            }} className={`py-1 text-[11px] font-semibold rounded-lg border text-center transition-colors cursor-pointer ${minRating === stars
                ? 'bg-indigo-50 border-indigo-300 text-indigo-700'
                : 'border-slate-200 text-slate-600 hover:bg-slate-50'}`}>
                    {stars === 0 ? 'Any' : `${stars}★`}
                  </button>))}
              </div>
            </div>

            {/* Language */}
            <div>
              <label className="block text-xs font-semibold text-slate-800 mb-2">Language</label>
              <select value={selectedLanguage} onChange={(e) => {
            setSelectedLanguage(e.target.value);
            setPage(0);
        }} className="w-full text-xs border border-slate-200 rounded-xl p-2 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500">
                <option value="">Any Language</option>
                {languagesList.map((lang) => (<option key={lang} value={lang}>
                    {lang}
                  </option>))}
              </select>
            </div>
          </aside>

          {/* Right Content Area: Sort Bar + Mentor Grid */}
          <main className="md:col-span-3 space-y-4">
            {/* Sort Dropdown & Summary Bar */}
            <div className="bg-white p-3.5 rounded-2xl border border-slate-200/80 shadow-2xs flex items-center justify-between gap-4">
              <span className="text-xs text-slate-500">
                Showing{' '}
                <strong className="text-slate-900">{mentors.length}</strong> of{' '}
                <strong className="text-slate-900">{totalElements}</strong> mentors
              </span>

              <div className="flex items-center gap-2">
                <span className="text-xs font-medium text-slate-500 hidden sm:inline">Sort by:</span>
                <select value={sortBy} onChange={(e) => {
            setSortBy(e.target.value);
            setPage(0);
        }} className="text-xs font-semibold border border-slate-200 rounded-xl px-2.5 py-1.5 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500">
                  <option value="relevance">Relevance</option>
                  <option value="rating">Highest Rated</option>
                  <option value="available_now">Available Now First</option>
                  <option value="price_asc">Price: Low to High</option>
                  <option value="price_desc">Price: High to Low</option>
                  <option value="experience">Most Experienced</option>
                </select>
              </div>
            </div>

            {/* Mentors Grid / Empty / Loading */}
            {isLoading ? (<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                {[1, 2, 3, 4, 5, 6].map((idx) => (<div key={idx} className="bg-white p-5 rounded-2xl border border-slate-200 animate-pulse space-y-3">
                    <div className="flex gap-3">
                      <div className="w-14 h-14 bg-slate-200 rounded-xl"></div>
                      <div className="flex-1 space-y-2">
                        <div className="h-4 bg-slate-200 rounded w-3/4"></div>
                        <div className="h-3 bg-slate-100 rounded w-1/2"></div>
                      </div>
                    </div>
                    <div className="h-10 bg-slate-50 rounded-xl"></div>
                    <div className="h-4 bg-slate-100 rounded w-full"></div>
                  </div>))}
              </div>) : mentors.length === 0 ? (<div className="bg-white rounded-2xl border border-slate-200 p-12 text-center">
                <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto mb-3">
                  <Search className="w-6 h-6"/>
                </div>
                <h3 className="text-base font-bold text-slate-900 mb-1">No mentors found</h3>
                <p className="text-xs text-slate-500 max-w-sm mx-auto mb-4">
                  We couldn't find any mentors matching your exact filters. Try broadening your search or resetting filters.
                </p>
                <Button variant="outline" size="sm" onClick={handleResetFilters}>
                  Reset All Filters
                </Button>
              </div>) : (<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
                {mentors.map((mentor) => (<MentorCard key={mentor.id} mentor={mentor} onBookClick={(m) => setSelectedBookingMentor(m)}/>))}
              </div>)}

            {/* Pagination Controls */}
            {totalPages > 1 && (<div className="pt-4 flex items-center justify-center gap-2">
                <Button size="sm" variant="outline" disabled={page <= 0} onClick={() => setPage((p) => Math.max(0, p - 1))} className="gap-1 px-3">
                  <ChevronLeft className="w-4 h-4"/>
                  Previous
                </Button>
                <span className="text-xs font-semibold text-slate-600 px-3">
                  Page {page + 1} of {totalPages}
                </span>
                <Button size="sm" variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)} className="gap-1 px-3">
                  Next
                  <ChevronRight className="w-4 h-4"/>
                </Button>
              </div>)}
          </main>
        </div>
      </div>

      {/* Booking Modal */}
      <BookingModal mentor={selectedBookingMentor} isOpen={Boolean(selectedBookingMentor)} onClose={() => setSelectedBookingMentor(null)}/>
    </div>);
};
export default MentorDiscoveryPage;
