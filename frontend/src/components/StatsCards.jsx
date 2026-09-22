import React from 'react';
import { Files, AlertTriangle, CheckCircle, Clock } from 'lucide-react';

export default function StatsCards({ documents = [] }) {
  const total = documents.length;
  const completed = documents.filter((d) => d.processingStatus === 'COMPLETED').length;
  const processing = documents.filter((d) => d.processingStatus === 'PROCESSING').length;
  const highRisk = documents.filter((d) => d.riskScore === 'High').length;

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      
      {/* Total Docs */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 shadow-lg flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400 font-medium">Total Ingested</p>
          <p className="text-2xl font-display font-bold text-white mt-1">{total}</p>
        </div>
        <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 flex items-center justify-center">
          <Files className="w-5 h-5" />
        </div>
      </div>

      {/* Completed */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 shadow-lg flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400 font-medium">AI Analyzed</p>
          <p className="text-2xl font-display font-bold text-emerald-400 mt-1">{completed}</p>
        </div>
        <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center">
          <CheckCircle className="w-5 h-5" />
        </div>
      </div>

      {/* Processing */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 shadow-lg flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400 font-medium">In Pipeline</p>
          <p className="text-2xl font-display font-bold text-sky-400 mt-1">{processing}</p>
        </div>
        <div className="w-10 h-10 rounded-xl bg-sky-500/10 border border-sky-500/20 text-sky-400 flex items-center justify-center">
          <Clock className="w-5 h-5" />
        </div>
      </div>

      {/* High Risk Flags */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 shadow-lg flex items-center justify-between">
        <div>
          <p className="text-xs text-slate-400 font-medium">High Risk Alerts</p>
          <p className="text-2xl font-display font-bold text-rose-400 mt-1">{highRisk}</p>
        </div>
        <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 flex items-center justify-center">
          <AlertTriangle className="w-5 h-5" />
        </div>
      </div>

    </div>
  );
}
