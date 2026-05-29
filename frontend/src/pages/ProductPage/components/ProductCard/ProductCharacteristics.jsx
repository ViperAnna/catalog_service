import React from 'react';
import {FiPlus, FiX} from 'react-icons/fi';

const ProductCharacteristics = ({isEditing, characteristics, isSubmitting, onChange}) => {
    const attrs = characteristics?.attributes || {};
    const entries = Object.entries(attrs).filter(([k]) => k !== '—');

    if (!isEditing) {
        if (entries.length === 0) return null;
        return (
            <div className="mb-6">
                <h4 className="text-sm font-semibold text-gray-700 mb-3">Характеристики</h4>
                <div className="rounded-lg border border-gray-100 overflow-hidden">
                    {entries.map(([key, value], i) => (
                        <div
                            key={key}
                            className={`flex px-4 py-2.5 text-sm ${i % 2 === 0 ? 'bg-gray-50' : 'bg-white'}`}
                        >
                            <span className="font-medium text-gray-600 w-1/2">{key}</span>
                            <span className="text-gray-800 w-1/2">{value}</span>
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    const pairs = entries.length > 0
        ? entries.map(([key, value]) => ({key, value}))
        : [{key: '', value: ''}];

    const handleChange = (idx, field, val) => {
        const next = [...pairs];
        next[idx] = {...next[idx], [field]: val};
        const newAttrs = {};
        next.forEach(({key, value}) => {
            if (key.trim()) newAttrs[key.trim()] = value;
        });
        onChange({attributes: newAttrs});
    };

    const addRow = () => {
        const newAttrs = {...attrs, '': ''};
        onChange({attributes: newAttrs});
    };

    const removeRow = (idx) => {
        const next = pairs.filter((_, i) => i !== idx);
        const newAttrs = {};
        next.forEach(({key, value}) => {
            if (key.trim()) newAttrs[key.trim()] = value;
        });
        onChange({attributes: newAttrs});
    };

    return (
        <div className="mb-6">
            <h4 className="text-sm font-semibold text-gray-700 mb-3">Характеристики</h4>
            <div className="space-y-2">
                {pairs.map(({key, value}, idx) => (
                    <div key={idx} className="flex gap-2 items-center">
                        <input
                            type="text"
                            value={key}
                            onChange={e => handleChange(idx, 'key', e.target.value)}
                            placeholder="Параметр"
                            disabled={isSubmitting}
                            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none disabled:opacity-50"
                        />
                        <input
                            type="text"
                            value={value}
                            onChange={e => handleChange(idx, 'value', e.target.value)}
                            placeholder="Значение"
                            disabled={isSubmitting}
                            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 outline-none disabled:opacity-50"
                        />
                        <button
                            type="button"
                            onClick={() => removeRow(idx)}
                            disabled={pairs.length === 1 || isSubmitting}
                            className="p-2 text-gray-400 hover:text-red-500 disabled:opacity-30 transition-colors"
                        >
                            <FiX className="w-4 h-4"/>
                        </button>
                    </div>
                ))}
                <button
                    type="button"
                    onClick={addRow}
                    disabled={isSubmitting}
                    className="flex items-center gap-2 text-sm text-emerald-600 hover:text-emerald-800 font-medium disabled:opacity-50"
                >
                    <FiPlus className="w-4 h-4"/>Добавить характеристику
                </button>
            </div>
        </div>
    );
};

export default ProductCharacteristics;
