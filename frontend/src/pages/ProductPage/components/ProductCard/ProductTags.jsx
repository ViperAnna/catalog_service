import React, {useState} from 'react';
import {FiTag, FiX} from 'react-icons/fi';

const ProductTags = ({isEditing, tags, isSubmitting, onChange}) => {
    const [tagInput, setTagInput] = useState('');

    const handleKeyDown = (e) => {
        if ((e.key === 'Enter' || e.key === ',') && tagInput.trim()) {
            e.preventDefault();
            const newTag = tagInput.trim().replace(/,$/, '');
            if (newTag && !tags.includes(newTag)) {
                onChange([...tags, newTag]);
            }
            setTagInput('');
        }
    };

    const removeTag = (idx) => onChange(tags.filter((_, i) => i !== idx));

    if (!isEditing) {
        if (!tags?.length) return null;
        return (
            <div className="mb-4">
                <h4 className="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-1">
                    <FiTag className="w-4 h-4"/>Теги
                </h4>
                <div className="flex flex-wrap gap-2">
                    {tags.map((tag, i) => (
                        <span key={i}
                              className="px-3 py-1 bg-emerald-50 text-emerald-700 rounded-full text-sm font-medium">
                            {tag}
                        </span>
                    ))}
                </div>
            </div>
        );
    }

    return (
        <div className="mb-6">
            <h4 className="text-sm font-semibold text-gray-700 mb-2">Теги</h4>
            <div className="border border-gray-300 rounded-lg p-3 focus-within:ring-2 focus-within:ring-emerald-500 focus-within:border-emerald-500 transition-all">
                <div className="flex flex-wrap gap-2 mb-2">
                    {tags.map((tag, i) => (
                        <span key={i}
                              className="inline-flex items-center gap-1 px-3 py-1 bg-emerald-50 text-emerald-700 rounded-full text-sm font-medium">
                            {tag}
                            <button type="button" onClick={() => removeTag(i)} disabled={isSubmitting}>
                                <FiX className="w-3 h-3"/>
                            </button>
                        </span>
                    ))}
                </div>
                <input
                    type="text"
                    value={tagInput}
                    onChange={e => setTagInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    disabled={isSubmitting}
                    placeholder="Введите тег (Enter для добавления)..."
                    className="w-full outline-none text-sm text-gray-700 placeholder-gray-400 disabled:opacity-50"
                />
            </div>
        </div>
    );
};

export default ProductTags;
