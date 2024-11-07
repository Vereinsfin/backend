import * as icons from '@radix-ui/react-icons';

type RemoveIconSuffix<T> = T extends `${infer U}Icon` ? U : T;
export type RadixIcons = RemoveIconSuffix<keyof typeof icons>;

export interface IconProps {
    icon: RadixIcons;
    size?: number | 'sm' | 'md' | 'lg';
    className?: string;
}

function Icon({ icon, ...props }: IconProps) {
    const IconComponent = icons[(icon + 'Icon') as keyof typeof icons];

    if (!IconComponent) {
        console.error(`Icon "${icon}" not found`);
        return null;
    }

    const size = { width: 15, height: 15 };
    if (props.size && typeof props.size === 'number') {
        size.width = props.size;
        size.height = props.size;
    } else if (props.size === 'sm') {
        size.width = 8;
        size.height = 8;
    } else if (props.size === 'md') {
        size.width = 24;
        size.height = 24;
    } else if (props.size === 'lg') {
        size.width = 36;
        size.height = 36;
    }

    return <IconComponent {...props} {...size} />;
}

export default Icon;
