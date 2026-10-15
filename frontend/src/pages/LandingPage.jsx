import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../components/common/Button';
import { MentorCard } from '../components/mentor/MentorCard';
import { BookingModal } from '../components/booking/BookingModal';
import api from '../api/client';
import { Search, ArrowRight, ShieldCheck, Zap, Users, Sparkles, ChevronDown, ChevronUp, Code, GraduationCap, Languages, Briefcase, TrendingUp, Palette, Music, HeartPulse, } from 'lucide-react';
export const LandingPage = () => {
    const [searchQuery, setSearchQuery] = useState('');
    const [popularMentors, setPopularMentors] = useState([]);
    const [categories, setCategories] = useState([]);
    const [selectedBookingMentor, setSelectedBookingMentor] = useState(null);
    const [openFaq, setOpenFaq] = useState(null);
    const navigate = useNavigate();
    useEffect(() => {
        // Fetch featured mentors
        api.get('/mentors?size=6&sortBy=rating')
            .then((res) => {
            if (res.data?.success && res.data.data?.content) {
                setPopularMentors(res.data.data.content);
            }
        })
            .catch(() => { });
        // Fetch categories
        api.get('/categories')
            .then((res) => {
            if (res.data?.success && res.data.data) {
                setCategories(res.data.data);
            }
        })
            .catch(() => { });
    }, []);
    const handleSearchSubmit = (e) => {
        e.preventDefault();
        if (searchQuery.trim()) {
            navigate(`/mentors?query=${encodeURIComponent(searchQuery.trim())}`);
        }
        else {
            navigate('/mentors');
        }
    };
    const sampleTags = ['Spring Boot', 'Physics', 'DSA', 'React', 'English', 'Startup', 'Guitar', 'Maths'];
    const categoryIcons = {
        programming: Code,
        'school-education': GraduationCap,
        languages: Languages,
        career: Briefcase,
        business: TrendingUp,
        design: Palette,
        music: Music,
        fitness: HeartPulse,
    };
    const faqs = [
        {
            q: 'Who can be a mentor on Mentivo?',
            a: 'Anyone with genuine, verifiable knowledge! You do not need to be a corporate executive—students can mentor school subjects, developers can teach frameworks, and hobbyists can teach guitar or languages. Every mentor is verified for safety.',
        },
        {
            q: 'Can I be both a learner and a mentor with the same account?',
            a: 'Yes! Mentivo is built as a two-sided knowledge marketplace. When signing up, choose "Both" (or switch anytime with one click in the header). Learn physics in the morning, and teach Java in the evening.',
        },
        {
            q: 'How does pricing and payment work?',
            a: 'Mentors set their transparent hourly rates. When you book a 15, 30, 45, or 60-minute session, the system automatically calculates the exact price and platform fee. Payments are processed securely with 256-bit encryption.',
        },
        {
            q: 'How are double-bookings prevented?',
            a: 'Our backend utilizes transactional conflict detection to guarantee mentors can never be booked for overlapping slots.',
        },
        {
            q: 'How do video sessions take place?',
            a: 'Every confirmed booking receives an encrypted, private meeting room link accessible directly through your Mentivo dashboard.',
        },
    ];
    return (<div className="min-h-screen bg-slate-50">
      {/* 1. HERO SECTION */}
      <section className="relative pt-12 pb-20 md:pt-20 md:pb-32 overflow-hidden bg-gradient-to-b from-indigo-50/70 via-white to-slate-50">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 text-center">
          {/* Tag Pill */}
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-semibold bg-indigo-100/80 text-indigo-800 border border-indigo-200 mb-6 shadow-2xs">
            <Sparkles className="w-3.5 h-3.5 text-indigo-600"/>
            <span>Learn from anyone. Teach what you know.</span>
          </div>

          {/* Headline */}
          <h1 className="text-4xl sm:text-5xl md:text-6xl font-black text-slate-900 tracking-tight leading-[1.15] mb-6">
            Need help?{' '}
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-indigo-500">
              Find someone who knows it.
            </span>
          </h1>

          {/* Subheading */}
          <p className="text-base sm:text-lg text-slate-600 max-w-2xl mx-auto mb-10 leading-relaxed">
            Connect with real mentors, experts, and skilled people whenever you need to learn, solve a tough problem, prepare for interviews, or master a new passion.
          </p>

          {/* Interactive Search Bar */}
          <form onSubmit={handleSearchSubmit} className="max-w-2xl mx-auto bg-white rounded-2xl p-2 sm:p-2.5 shadow-xl shadow-indigo-100/50 border border-slate-200 flex flex-col sm:flex-row items-center gap-2 mb-4">
            <div className="flex items-center gap-2 px-3 w-full sm:flex-1">
              <Search className="w-5 h-5 text-slate-400 shrink-0"/>
              <input type="text" value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} placeholder="What do you want to learn? (e.g., Spring Boot, Physics, DSA...)" className="w-full text-sm text-slate-900 placeholder:text-slate-400 bg-transparent focus:outline-none"/>
            </div>
            <Button size="md" variant="primary" type="submit" className="w-full sm:w-auto px-6 gap-2">
              <span>Find Mentor</span>
              <ArrowRight className="w-4 h-4"/>
            </Button>
          </form>

          {/* Popular Tag Pills */}
          <div className="flex items-center justify-center gap-1.5 flex-wrap text-xs text-slate-500 max-w-xl mx-auto">
            <span className="font-semibold text-slate-700">Popular:</span>
            {sampleTags.map((tag) => (<button key={tag} onClick={() => navigate(`/mentors?query=${encodeURIComponent(tag)}`)} className="px-2.5 py-1 rounded-lg bg-white border border-slate-200 text-slate-600 hover:border-indigo-400 hover:text-indigo-600 transition-colors cursor-pointer">
                {tag}
              </button>))}
          </div>

          {/* Dual Action CTAs */}
          <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-3">
            <Link to="/mentors">
              <Button size="lg" variant="primary" className="px-8 shadow-md shadow-indigo-200">
                Find a Mentor
              </Button>
            </Link>
            <Link to="/register">
              <Button size="lg" variant="outline" className="px-8 bg-white">
                Become a Mentor
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* 2. HOW MENTIVO WORKS */}
      <section className="py-20 bg-white border-y border-slate-200/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <h2 className="text-xs font-bold text-indigo-600 tracking-wider uppercase mb-2">Simple & Intuitive Flow</h2>
            <p className="text-3xl font-extrabold text-slate-900 tracking-tight">How Mentivo Works</p>
            <p className="text-sm text-slate-600 mt-2">
              From finding help to completing your 1-on-1 session in three transparent steps.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 text-center relative group hover:bg-white hover:shadow-lg transition-all duration-200">
              <div className="w-12 h-12 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-bold text-lg mx-auto mb-4 shadow-sm shadow-indigo-200">
                1
              </div>
              <h3 className="font-bold text-slate-900 text-base mb-2">Search & Filter</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Search by subject, topic, language, hourly budget, or availability. View real online statuses and verified ratings.
              </p>
            </div>

            <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 text-center relative group hover:bg-white hover:shadow-lg transition-all duration-200">
              <div className="w-12 h-12 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-bold text-lg mx-auto mb-4 shadow-sm shadow-indigo-200">
                2
              </div>
              <h3 className="font-bold text-slate-900 text-base mb-2">Connect & Chat</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Send a quick connection request or chat in real time. Align on your questions and schedule a convenient session slot.
              </p>
            </div>

            <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 text-center relative group hover:bg-white hover:shadow-lg transition-all duration-200">
              <div className="w-12 h-12 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-bold text-lg mx-auto mb-4 shadow-sm shadow-indigo-200">
                3
              </div>
              <h3 className="font-bold text-slate-900 text-base mb-2">Book, Learn & Review</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Transparent server-calculated pricing. Meet securely over video, solve your problem, and share authentic feedback.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 3. EXPLORE CATEGORIES */}
      <section className="py-20 bg-slate-50" id="categories">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col sm:flex-row items-start sm:items-end justify-between mb-12 gap-4">
            <div>
              <h2 className="text-xs font-bold text-indigo-600 tracking-wider uppercase mb-1">Knowledge Marketplace</h2>
              <p className="text-3xl font-extrabold text-slate-900 tracking-tight">Explore Categories</p>
            </div>
            <Link to="/mentors" className="text-xs font-bold text-indigo-600 hover:text-indigo-700 flex items-center gap-1">
              <span>View all mentors</span>
              <ArrowRight className="w-4 h-4"/>
            </Link>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {categories.map((cat) => {
            const Icon = categoryIcons[cat.slug] || Sparkles;
            return (<div key={cat.id} onClick={() => navigate(`/mentors?categoryId=${cat.id}`)} className="bg-white p-5 rounded-2xl border border-slate-200 hover:border-indigo-300 hover:shadow-md transition-all cursor-pointer group">
                  <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center mb-3 group-hover:bg-indigo-600 group-hover:text-white transition-colors">
                    <Icon className="w-5 h-5"/>
                  </div>
                  <h3 className="text-sm font-bold text-slate-900 mb-1 group-hover:text-indigo-600 transition-colors">
                    {cat.name}
                  </h3>
                  <p className="text-[11px] text-slate-500 line-clamp-2 leading-relaxed">
                    {cat.description}
                  </p>
                </div>);
        })}
          </div>
        </div>
      </section>

      {/* 4. POPULAR / FEATURED MENTORS */}
      {popularMentors.length > 0 && (<section className="py-20 bg-white border-t border-slate-200">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex flex-col sm:flex-row items-start sm:items-end justify-between mb-12 gap-4">
              <div>
                <h2 className="text-xs font-bold text-indigo-600 tracking-wider uppercase mb-1">Top Rated Experts</h2>
                <p className="text-3xl font-extrabold text-slate-900 tracking-tight">Learn from the Best</p>
                <p className="text-sm text-slate-600 mt-1">Verified mentors ready for 1-on-1 sessions.</p>
              </div>
              <Link to="/mentors" className="text-xs font-bold text-indigo-600 hover:text-indigo-700 flex items-center gap-1">
                <span>Browse all {popularMentors.length}+ mentors</span>
                <ArrowRight className="w-4 h-4"/>
              </Link>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {popularMentors.map((mentor) => (<MentorCard key={mentor.id} mentor={mentor} onBookClick={(m) => setSelectedBookingMentor(m)}/>))}
            </div>
          </div>
        </section>)}

      {/* 5. PHILOSOPHY & EARNING SECTION */}
      <section className="py-20 bg-gradient-to-tr from-slate-900 to-indigo-950 text-white relative overflow-hidden">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center">
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight mb-4">
            Anyone can teach. Anyone can learn.
          </h2>
          <p className="text-slate-300 text-sm sm:text-base max-w-2xl mx-auto mb-10 leading-relaxed">
            Whether you are a college student helping high schoolers with calculus, or a principal architect guiding senior engineers, Mentivo gives you the platform, scheduling, and payment tools to monetize your expertise.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 text-left mb-10">
            <div className="p-5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-sm">
              <ShieldCheck className="w-6 h-6 text-indigo-400 mb-2"/>
              <h3 className="font-bold text-sm text-white mb-1">Guaranteed Payouts</h3>
              <p className="text-xs text-slate-300">
                Transparent 20% platform commission. Keep 80% of every rupee earned with automated session tracking.
              </p>
            </div>

            <div className="p-5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-sm">
              <Zap className="w-6 h-6 text-indigo-400 mb-2"/>
              <h3 className="font-bold text-sm text-white mb-1">Set Your Schedule</h3>
              <p className="text-xs text-slate-300">
                Define weekly availability, go online with a toggle, and take bookings only when you want to.
              </p>
            </div>

            <div className="p-5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-sm">
              <Users className="w-6 h-6 text-indigo-400 mb-2"/>
              <h3 className="font-bold text-sm text-white mb-1">Build Your Reputation</h3>
              <p className="text-xs text-slate-300">
                Collect verified reviews from completed sessions to unlock higher verification tiers and premium rates.
              </p>
            </div>
          </div>

          <Link to="/register">
            <Button size="lg" variant="primary" className="bg-indigo-500 hover:bg-indigo-600 px-8">
              Start Mentoring Today
            </Button>
          </Link>
        </div>
      </section>

      {/* 6. FAQ ACCORDION */}
      <section className="py-20 bg-slate-50">
        <div className="max-w-3xl mx-auto px-4 sm:px-6">
          <div className="text-center mb-12">
            <h2 className="text-xs font-bold text-indigo-600 tracking-wider uppercase mb-1">Got Questions?</h2>
            <p className="text-3xl font-extrabold text-slate-900 tracking-tight">Frequently Asked Questions</p>
          </div>

          <div className="space-y-3">
            {faqs.map((faq, idx) => {
            const isOpen = openFaq === idx;
            return (<div key={idx} className="bg-white rounded-2xl border border-slate-200 overflow-hidden transition-colors">
                  <button onClick={() => setOpenFaq(isOpen ? null : idx)} className="w-full p-4 text-left flex items-center justify-between font-bold text-sm text-slate-900 cursor-pointer">
                    <span>{faq.q}</span>
                    {isOpen ? (<ChevronUp className="w-4 h-4 text-slate-400 shrink-0"/>) : (<ChevronDown className="w-4 h-4 text-slate-400 shrink-0"/>)}
                  </button>
                  {isOpen && (<div className="px-4 pb-4 text-xs text-slate-600 leading-relaxed border-t border-slate-50 pt-2">
                      {faq.a}
                    </div>)}
                </div>);
        })}
          </div>
        </div>
      </section>

      {/* Booking Modal Instance */}
      <BookingModal mentor={selectedBookingMentor} isOpen={Boolean(selectedBookingMentor)} onClose={() => setSelectedBookingMentor(null)}/>
    </div>);
};
export default LandingPage;
