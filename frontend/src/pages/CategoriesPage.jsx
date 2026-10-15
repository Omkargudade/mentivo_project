import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { Button } from '../components/common/Button';
import {
  Code,
  GraduationCap,
  Briefcase,
  Languages,
  Palette,
  Music,
  Activity,
  Layers,
  Search,
  Users,
  Calendar,
  Sparkles,
  ArrowRight,
  BookOpen,
  Clock
} from 'lucide-react';

const iconMap = {
  code: Code,
  'graduation-cap': GraduationCap,
  briefcase: Briefcase,
  languages: Languages,
  palette: Palette,
  music: Music,
  activity: Activity,
  layers: Layers,
};

export const CategoriesPage = () => {
  const [categories, setCategories] = useState([]);
  const [topics, setTopics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [catsRes, topicsRes] = await Promise.all([
        api.get('/categories'),
        api.get('/topics')
      ]);

      if (catsRes.data?.success) {
        setCategories(catsRes.data.data || []);
      }
      if (topicsRes.data?.success) {
        setTopics(topicsRes.data.data || []);
      }
    } catch (err) {
      console.error('Error fetching categories:', err);
      setError('Failed to load categories. Please check your connection and try again.');
    } finally {
      setLoading(false);
    }
  };

  // Group topics by category ID
  const topicsByCategory = topics.reduce((acc, topic) => {
    const catId = topic.category?.id;
    if (catId) {
      if (!acc[catId]) acc[catId] = [];
      acc[catId].push(topic);
    }
    return acc;
  }, {});

  const filteredCategories = categories.filter((cat) => {
    const term = searchTerm.toLowerCase();
    const matchesName = cat.name.toLowerCase().includes(term);
    const matchesDesc = cat.description?.toLowerCase().includes(term);
    const catTopics = topicsByCategory[cat.id] || [];
    const matchesTopic = catTopics.some(t => t.name.toLowerCase().includes(term));
    return matchesName || matchesDesc || matchesTopic;
  });

  return (
    <div className="min-h-screen bg-slate-50 py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto space-y-8">
        {/* Header Banner */}
        <div className="bg-gradient-to-r from-indigo-900 via-indigo-800 to-indigo-950 rounded-3xl p-8 sm:p-12 text-white relative overflow-hidden shadow-xl">
          <div className="relative z-10 max-w-2xl space-y-4">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/20 text-indigo-200 border border-indigo-400/30 text-xs font-semibold backdrop-blur-sm">
              <Sparkles className="w-3.5 h-3.5 text-indigo-300" />
              <span>Explore Disciplines</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
              Browse Mentors & Sessions by Category
            </h1>
            <p className="text-indigo-200 text-sm sm:text-base leading-relaxed">
              Find experienced industry mentors, 1-on-1 personalized sessions, and interactive live webinars across diverse domains.
            </p>

            {/* Coming Soon Course Platform Banner */}
            <div className="pt-2 flex flex-wrap items-center gap-3">
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/10 backdrop-blur-md border border-white/20 text-white text-xs font-medium">
                <BookOpen className="w-4 h-4 text-amber-300" />
                <span>Full Video Courses & Modules</span>
                <span className="px-2 py-0.5 rounded-full bg-amber-400 text-slate-900 font-bold text-[10px] uppercase tracking-wider">
                  Coming Soon
                </span>
              </div>
            </div>
          </div>

          <div className="absolute right-0 top-0 bottom-0 w-1/3 opacity-10 pointer-events-none hidden lg:flex items-center justify-center">
            <Layers className="w-96 h-96 text-white" />
          </div>
        </div>

        {/* Search & Stats Bar */}
        <div className="flex flex-col sm:flex-row gap-4 items-center justify-between">
          <div className="relative w-full sm:w-96">
            <Search className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search category, topic, or keyword..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 bg-white text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm transition-all"
            />
          </div>

          <div className="flex items-center gap-3 text-xs text-slate-500 font-medium w-full sm:w-auto justify-end">
            <span>Showing <strong>{filteredCategories.length}</strong> categories</span>
          </div>
        </div>

        {/* Loading State */}
        {loading && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((idx) => (
              <div key={idx} className="bg-white rounded-2xl border border-slate-200/80 p-6 animate-pulse space-y-4">
                <div className="w-12 h-12 rounded-xl bg-slate-100" />
                <div className="h-5 bg-slate-100 rounded w-1/2" />
                <div className="h-4 bg-slate-100 rounded w-full" />
                <div className="h-4 bg-slate-100 rounded w-3/4" />
                <div className="pt-4 flex gap-2">
                  <div className="h-9 bg-slate-100 rounded-lg flex-1" />
                  <div className="h-9 bg-slate-100 rounded-lg flex-1" />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Error State */}
        {!loading && error && (
          <div className="rounded-2xl border border-rose-200 bg-rose-50/70 p-8 text-center space-y-3">
            <p className="text-sm font-semibold text-rose-800">{error}</p>
            <Button variant="outline" size="sm" onClick={fetchData}>
              Try Again
            </Button>
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && filteredCategories.length === 0 && (
          <div className="rounded-3xl border border-dashed border-slate-300 bg-white p-12 text-center space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto">
              <Layers className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-800">No categories found</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              No categories match your search keyword &ldquo;{searchTerm}&rdquo;. Try another term or clear the filter.
            </p>
            {searchTerm && (
              <Button variant="outline" size="sm" onClick={() => setSearchTerm('')}>
                Clear Search
              </Button>
            )}
          </div>
        )}

        {/* Category Grid */}
        {!loading && !error && filteredCategories.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredCategories.map((category) => {
              const IconComponent = iconMap[category.iconName] || Layers;
              const catTopics = topicsByCategory[category.id] || [];

              return (
                <div
                  key={category.id}
                  className="bg-white rounded-2xl border border-slate-200/80 p-6 flex flex-col justify-between hover:border-indigo-300 hover:shadow-lg transition-all duration-200 group"
                >
                  <div className="space-y-4">
                    <div className="flex items-start justify-between">
                      <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center group-hover:bg-indigo-600 group-hover:text-white transition-colors">
                        <IconComponent className="w-6 h-6" />
                      </div>
                      <span className="text-[11px] font-semibold text-slate-400">
                        {category.displayOrder ? `#${category.displayOrder}` : ''}
                      </span>
                    </div>

                    <div>
                      <h3 className="text-lg font-bold text-slate-900 group-hover:text-indigo-600 transition-colors">
                        {category.name}
                      </h3>
                      {category.description && (
                        <p className="text-xs text-slate-500 mt-1 line-clamp-2 leading-relaxed">
                          {category.description}
                        </p>
                      )}
                    </div>

                    {/* Topics badges */}
                    {catTopics.length > 0 && (
                      <div className="pt-2">
                        <p className="text-[11px] font-semibold text-slate-400 mb-2 uppercase tracking-wider">
                          Key Topics
                        </p>
                        <div className="flex flex-wrap gap-1.5">
                          {catTopics.slice(0, 4).map((topic) => (
                            <span
                              key={topic.id}
                              className="px-2.5 py-1 rounded-lg bg-slate-50 text-slate-700 text-[11px] font-medium border border-slate-100"
                            >
                              {topic.name}
                            </span>
                          ))}
                          {catTopics.length > 4 && (
                            <span className="px-2 py-1 rounded-lg bg-slate-50 text-slate-400 text-[11px] font-medium">
                              +{catTopics.length - 4} more
                            </span>
                          )}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="pt-6 border-t border-slate-100 mt-6 grid grid-cols-2 gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className="w-full text-xs flex items-center justify-center gap-1.5 border-slate-200 hover:border-indigo-300 hover:bg-indigo-50/50"
                      onClick={() => navigate(`/mentors?categoryId=${category.id}`)}
                    >
                      <Users className="w-3.5 h-3.5 text-indigo-600" />
                      <span>Mentors</span>
                    </Button>

                    <Button
                      variant="primary"
                      size="sm"
                      className="w-full text-xs flex items-center justify-center gap-1.5"
                      onClick={() => navigate(`/sessions?categoryId=${category.id}`)}
                    >
                      <Calendar className="w-3.5 h-3.5" />
                      <span>Sessions</span>
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Roadmap / Courses Coming Soon Section */}
        <div className="mt-12 bg-white rounded-3xl border border-slate-200/90 p-8 sm:p-10 shadow-sm">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
            <div className="space-y-2 max-w-2xl">
              <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-50 text-amber-700 border border-amber-200 text-xs font-semibold">
                <BookOpen className="w-3.5 h-3.5" />
                <span>Future Learning Roadmap</span>
              </div>
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900">
                Self-Paced Courses & Certification Programs
              </h2>
              <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
                Structured courses with video lessons, code repositories, downloadable resources, and completion certificates will be hosted directly on Mentivo in an upcoming release.
              </p>
            </div>
            <div className="flex-shrink-0">
              <button
                disabled
                className="px-5 py-2.5 rounded-xl bg-slate-100 text-slate-400 font-semibold text-xs border border-slate-200 cursor-not-allowed flex items-center gap-2 shadow-2xs"
                title="Courses platform is planned for a future release"
              >
                <Clock className="w-4 h-4 text-slate-400" />
                <span>Courses — Coming Soon</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CategoriesPage;
