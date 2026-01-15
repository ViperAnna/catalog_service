import {useAutoAnimate} from '@formkit/auto-animate/react';
import CategoryCarousel from './CategoryCarousel.jsx';
import CategoryGrid from './CategoryGrid.jsx';

const CategoryList = ({
                          categories,
                          showAll,
                          scrollRef,
                          scrollLeft,
                          scrollRight
                      }) => {
    const [parent] = useAutoAnimate({duration: 300});

    return (
        <div ref={parent}>
            {!showAll ? (
                <CategoryCarousel
                    categories={categories}
                    scrollRef={scrollRef}
                    scrollLeft={scrollLeft}
                    scrollRight={scrollRight}
                />
            ) : (
                <CategoryGrid categories={categories}/>
            )}
        </div>
    );
};

export default CategoryList;