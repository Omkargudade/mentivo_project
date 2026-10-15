import React from 'react';
import { Link } from 'react-router-dom';
import { Sparkles, Heart } from 'lucide-react';
export const Footer = () => {
    return (<footer className="bg-white border-t border-slate-200 mt-20 pt-16 pb-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-8 mb-12">
          {/* Brand Col */}
          <div className="col-span-2">
            <Link to="/" className="flex items-center gap-2 mb-3">
              <div className="w-8 h-8 rounded-xl bg-indigo-600 flex items-center justify-center text-white">
                <Sparkles className="w-4 h-4"/>
              </div>
              <span className="text-xl font-black text-slate-900 tracking-tight">MENTIVO</span>
            </Link>
            <p className="text-sm text-slate-600 font-medium mb-2">
              Learn from anyone. Teach what you know.
            </p>
            <p className="text-xs text-slate-500 max-w-sm leading-relaxed">
              Mentivo is a two-sided knowledge marketplace connecting curious learners with real people who know their craft—from software engineering and school physics to languages, music, and startup advice.
            </p>
          </div>

          {/* Explore */}
          <div>
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-3">Explore</h4>
            <ul className="space-y-2 text-xs text-slate-600">
              <li><Link to="/mentors?categoryId=1" className="hover:text-indigo-600 transition-colors">Programming</Link></li>
              <li><Link to="/mentors?categoryId=2" className="hover:text-indigo-600 transition-colors">School Education</Link></li>
              <li><Link to="/mentors?categoryId=3" className="hover:text-indigo-600 transition-colors">Languages</Link></li>
              <li><Link to="/mentors?categoryId=4" className="hover:text-indigo-600 transition-colors">Career & Interview</Link></li>
              <li><Link to="/mentors?categoryId=5" className="hover:text-indigo-600 transition-colors">Startup & Business</Link></li>
            </ul>
          </div>

          {/* Platform */}
          <div>
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-3">Platform</h4>
            <ul className="space-y-2 text-xs text-slate-600">
              <li><Link to="/how-it-works" className="hover:text-indigo-600 transition-colors">How It Works</Link></li>
              <li><Link to="/mentors" className="hover:text-indigo-600 transition-colors">Find a Mentor</Link></li>
              <li><Link to="/register" className="hover:text-indigo-600 transition-colors">Become a Mentor</Link></li>
              <li><Link to="/login" className="hover:text-indigo-600 transition-colors">Sign In</Link></li>
            </ul>
          </div>

          {/* Trust & Safety */}
          <div>
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-3">Safety & Trust</h4>
            <ul className="space-y-2 text-xs text-slate-600">
              <li><span className="text-slate-500">Verified Mentors</span></li>
              <li><span className="text-slate-500">Secure Payments</span></li>
              <li><span className="text-slate-500">Double-Booking Guard</span></li>
              <li><span className="text-slate-500">Authentic Reviews</span></li>
            </ul>
          </div>
        </div>

        <div className="pt-8 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-4">
          <p>© {new Date().getFullYear()} Mentivo Technologies Inc. All rights reserved.</p>
          <div className="flex items-center gap-1">
            <span>Built with</span>
            <Heart className="w-3.5 h-3.5 text-rose-500 fill-rose-500"/>
            <span>for learners and mentors worldwide</span>
          </div>
        </div>
      </div>
    </footer>);
};
export default Footer;
