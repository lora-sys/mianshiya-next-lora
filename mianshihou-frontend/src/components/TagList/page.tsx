import "./index.css";
import { Tag } from "antd";

interface Props {
  tagList?: string[];
}

const TagList = (props: Props) => {
  const { tagList = [] } = props;

  return (
    <div id="tag-list">
      {tagList.map((tag) => {
        return <Tag key={tag}>{tag}</Tag>;
      })}
    </div>
  );
};

export default TagList;
