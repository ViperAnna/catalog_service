import {FiChevronLeft, FiChevronRight} from 'react-icons/fi';
import CategoryCard from './CategoryCard.jsx';

const CategoryCarousel = ({categories, scrollRef, scrollLeft, scrollRight}) => (
    <div className="relative">
        <div
            ref={scrollRef}
            className="flex overflow-x-auto scrollbar-hide space-x-6 pb-4 snap-x snap-mandatory"
            style={{scrollbarWidth: 'none', msOverflowStyle: 'none'}}
        >
            {categories.map((category) => (
                <div key={category.id} className="flex-shrink-0 w-64 snap-start h-full">
                    <CategoryCard category={category} compact={true}/>
                </div>
            ))}
        </div>

        <ScrollButton
            direction="left"
            onClick={scrollLeft}
            icon={FiChevronLeft}
        />
        <ScrollButton
            direction="right"
            onClick={scrollRight}
            icon={FiChevronRight}
        />
    </div>
);

const ScrollButton = ({direction, onClick, icon: Icon}) => (
    <button
        onClick={onClick}
        className={`absolute ${direction === 'left' ? 'left-0' : 'right-0'} top-0 bottom-4 flex items-center justify-${direction === 'left' ? 'start' : 'end'} z-20 w-16 group transition-all duration-200`}
        aria-label={`Прокрутить в${direction === 'left' ? 'лево' : 'право'}`}
    >
        <div className="relative h-full w-64">

            {direction === 'left' && (
                <div className="absolute top-0 left-0 h-full w-16
                    bg-gradient-to-r from-transparent via-black/0 to-black/0
                    group-hover:bg-gradient-to-r group-hover:from-transparent group-hover:via-black/20 group-hover:to-black/30
                    rounded-l-xl transition-all duration-200"></div>
            )}

            {direction === 'right' && (
                <div className="absolute top-0 right-0 h-full w-16
                    bg-gradient-to-l from-black/0 via-black/0 to-transparent
                    group-hover:bg-gradient-to-l group-hover:from-black/30 group-hover:via-black/20 group-hover:to-transparent
                    rounded-r-xl transition-all duration-200"></div>
            )}

            <div className={`absolute ${direction === 'left' ? 'left-4' : 'right-4'} top-1/2 transform -translate-y-1/2
                p-3 rounded-full bg-white/90 backdrop-blur-sm shadow-lg
                group-hover:shadow-xl group-hover:scale-110
                group-hover:bg-emerald-50/90 group-hover:border-emerald-200
                border border-gray-200/50 transition-all duration-200`}>
                <Icon
                    className="w-6 h-6 text-gray-700 group-hover:text-emerald-600 group-hover:scale-105 transition-transform duration-200"/>
            </div>
        </div>
    </button>
);
export default CategoryCarousel;